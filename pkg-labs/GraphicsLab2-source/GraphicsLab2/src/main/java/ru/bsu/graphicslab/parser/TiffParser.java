package ru.bsu.graphicslab.parser;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

final class TiffParser {
    private static final int SHORT = 3;
    private static final int LONG = 4;
    private static final int RATIONAL = 5;

    private TiffParser() {
    }

    static ImageInfo parse(BinaryFile file) throws IOException {
        Directory ifd = new Directory(file, 0, file.length(), false);
        long width = ifd.integer(256, 0);
        long height = ifd.integer(257, 0);
        if (width == 0 || height == 0) {
            throw new FormatException("TIFF: отсутствуют корректные размеры изображения");
        }

        long samples = ifd.shortValue(277, 1);
        int depth = ifd.depth(samples);
        long compression = ifd.shortValue(259, 1);
        ifd.checkImageData(compression);
        Dpi dpi = ifd.dpi();
        String order = ifd.littleEndian ? "II (Little Endian)" : "MM (Big Endian)";
        return new ImageInfo("TIFF", width, height, dpi.x(), dpi.y(), depth,
                compressionName(compression), order);
    }

    static Dpi readExifDpi(BinaryFile file, long start, long end) throws IOException {
        return new Directory(file, start, end, true).dpi();
    }

    record Dpi(Double x, Double y) {
    }

    private record Tag(int type, long count, long position) {
    }

    private static final class Directory {
        private final BinaryFile file;
        private final long start;
        private final long end;
        private final boolean littleEndian;
        private final Map<Integer, Tag> tags = new HashMap<>();

        Directory(BinaryFile file, long start, long end, boolean exif) throws IOException {
            if (start < 0 || end < start || end - start < 8 || end > file.length()) {
                throw new FormatException("TIFF: неполный заголовок");
            }
            this.file = file;
            this.start = start;
            this.end = end;
            file.seek(start);
            String order = file.ascii(2);
            if (!order.equals("II") && !order.equals("MM")) {
                throw new FormatException("TIFF: неизвестный порядок байтов");
            }
            littleEndian = order.equals("II");
            int magic = u16();
            if (magic != 42) {
                throw new FormatException(magic == 43
                        ? "TIFF: вариант BigTIFF не поддерживается"
                        : "TIFF: неверное число в заголовке");
            }

            long firstIfd = u32();
            file.seek(requireOffset(firstIfd, 2));
            int entries = u16();
            if (entries == 0) {
                throw new FormatException("TIFF: пустой каталог IFD");
            }
            requireOffset(firstIfd, 2L + 12L * entries + 4);
            for (int index = 0; index < entries; index++) {
                int number = u16();
                int type = u16();
                long count = u32();
                long valuePosition = file.position();
                long offset = u32();
                if (!wanted(number, exif)) {
                    continue;
                }

                int size = typeSize(type);
                if (size == 0 || count == 0) {
                    throw new FormatException("TIFF: неверный тип или количество значений тега " + number);
                }
                long bytes = size * count;
                // Значения длиной до четырёх байтов лежат прямо в записи IFD.
                long position = bytes <= 4 ? valuePosition : requireOffset(offset, bytes);
                if (tags.put(number, new Tag(type, count, position)) != null) {
                    throw new FormatException("TIFF: повторяющийся тег " + number);
                }
            }
            long nextIfd = u32();
            if (nextIfd != 0) {
                if (nextIfd == firstIfd) {
                    throw new FormatException("TIFF: циклическая ссылка на IFD");
                }
                requireOffset(nextIfd, 6);
            }
        }

        long integer(int number, long defaultValue) throws IOException {
            return scalar(number, defaultValue, false);
        }

        long shortValue(int number, long defaultValue) throws IOException {
            return scalar(number, defaultValue, true);
        }

        private long scalar(int number, long defaultValue, boolean shortOnly) throws IOException {
            Tag tag = tags.get(number);
            if (tag == null) {
                return defaultValue;
            }
            if (tag.count() != 1 || (tag.type() != SHORT && (shortOnly || tag.type() != LONG))) {
                throw new FormatException("TIFF: неверная запись тега " + number);
            }
            return integerAt(tag, 0);
        }

        int depth(long samples) throws IOException {
            if (samples == 0) {
                throw new FormatException("TIFF: SamplesPerPixel равно нулю");
            }
            Tag bits = tags.get(258);
            if (bits == null) {
                return (int) samples;
            }
            if (bits.type() != SHORT || bits.count() != samples) {
                throw new FormatException("TIFF: BitsPerSample не соответствует SamplesPerPixel");
            }
            long depth = 0;
            file.seek(bits.position());
            for (long index = 0; index < bits.count(); index++) {
                int component = u16();
                if (component == 0) {
                    throw new FormatException("TIFF: нулевая глубина компонента");
                }
                depth += component;
            }
            if (depth > Integer.MAX_VALUE) {
                throw new FormatException("TIFF: слишком большая глубина цвета");
            }
            return (int) depth;
        }

        Dpi dpi() throws IOException {
            long unit = shortValue(296, 2); // TIFF 6.0: по умолчанию дюймы.
            Double x = rational(282);
            Double y = rational(283);
            if (unit != 2 && unit != 3) {
                return new Dpi(null, null);
            }
            double factor = unit == 3 ? 2.54 : 1.0;
            return new Dpi(x == null ? null : x * factor, y == null ? null : y * factor);
        }

        private Double rational(int number) throws IOException {
            Tag tag = tags.get(number);
            if (tag == null) {
                return null;
            }
            if (tag.type() != RATIONAL || tag.count() != 1) {
                throw new FormatException("TIFF: неверная дробь в теге " + number);
            }
            file.seek(tag.position());
            long numerator = u32();
            long denominator = u32();
            if (denominator == 0) {
                throw new FormatException("TIFF: нулевой знаменатель разрешения");
            }
            return numerator == 0 ? null : (double) numerator / denominator;
        }

        void checkImageData(long compression) throws IOException {
            Tag strips = tags.get(273);
            Tag stripSizes = tags.get(279);
            Tag tiles = tags.get(324);
            Tag tileSizes = tags.get(325);
            if (strips != null || stripSizes != null) {
                checkRanges(strips, stripSizes, "полос");
            }
            if (tiles != null || tileSizes != null) {
                checkRanges(tiles, tileSizes, "плиток");
            }
            if (strips == null && tiles == null) {
                // Старый JPEG-in-TIFF может хранить самостоятельный JPEG-поток.
                long jpegOffset = integer(513, 0);
                long jpegLength = integer(514, 0);
                if (compression != 6 || jpegLength == 0) {
                    throw new FormatException("TIFF: отсутствуют данные полос или плиток");
                }
                requireOffset(jpegOffset, jpegLength);
            }
        }

        private void checkRanges(Tag offsets, Tag sizes, String name) throws IOException {
            if (offsets == null || sizes == null || offsets.count() != sizes.count()
                    || !isInteger(offsets) || !isInteger(sizes)) {
                throw new FormatException("TIFF: неверные таблицы смещений и размеров " + name);
            }
            long[] blockOffsets = new long[(int) Math.min(256, offsets.count())];
            for (long index = 0; index < offsets.count(); index += blockOffsets.length) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException();
                }
                int count = (int) Math.min(blockOffsets.length, offsets.count() - index);
                file.seek(offsets.position() + typeSize(offsets.type()) * index);
                for (int i = 0; i < count; i++) {
                    blockOffsets[i] = readInteger(offsets);
                }
                file.seek(sizes.position() + typeSize(sizes.type()) * index);
                for (int i = 0; i < count; i++) {
                    long size = readInteger(sizes);
                    if (size == 0) {
                        throw new FormatException("TIFF: нулевой размер блока изображения");
                    }
                    requireOffset(blockOffsets[i], size);
                }
            }
        }

        private long integerAt(Tag tag, long index) throws IOException {
            file.seek(tag.position() + typeSize(tag.type()) * index);
            return readInteger(tag);
        }

        private long readInteger(Tag tag) throws IOException {
            return tag.type() == SHORT ? u16() : u32();
        }

        private long requireOffset(long offset, long size) throws FormatException {
            long length = end - start;
            if (offset < 8 || offset > length || size > length - offset) {
                throw new FormatException("TIFF: смещение или длина выходят за границы данных");
            }
            return start + offset;
        }

        private int u16() throws IOException {
            return littleEndian ? file.u16LE() : file.u16BE();
        }

        private long u32() throws IOException {
            return littleEndian ? file.u32LE() : file.u32BE();
        }
    }

    private static boolean isInteger(Tag tag) {
        return tag.type() == SHORT || tag.type() == LONG;
    }

    private static int typeSize(int type) {
        return switch (type) {
            case 1, 2, 6, 7 -> 1;
            case 3, 8 -> 2;
            case 4, 9, 11, 13 -> 4;
            case 5, 10, 12 -> 8;
            default -> 0;
        };
    }

    private static boolean wanted(int number, boolean exif) {
        if (number == 282 || number == 283 || number == 296) {
            return true;
        }
        return !exif && switch (number) {
            case 256, 257, 258, 259, 273, 277, 279, 324, 325, 513, 514 -> true;
            default -> false;
        };
    }

    private static String compressionName(long compression) {
        return switch ((int) compression) {
            case 1 -> "Без сжатия";
            case 2 -> "CCITT RLE";
            case 3 -> "CCITT Group 3";
            case 4 -> "CCITT Group 4";
            case 5 -> "LZW";
            case 6 -> "JPEG (старый TIFF)";
            case 7 -> "JPEG";
            case 8, 32946 -> "Deflate";
            case 32773 -> "PackBits";
            default -> "Код TIFF " + compression;
        };
    }
}
