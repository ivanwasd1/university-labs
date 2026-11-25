package ru.bsu.graphicslab.parser;

import java.io.IOException;
import java.io.InterruptedIOException;

final class JpegParser {
    private static final int SOI = 0xD8;
    private static final int EOI = 0xD9;
    private static final int SOS = 0xDA;
    private static final int DNL = 0xDC;
    private static final int APP0 = 0xE0;
    private static final int APP1 = 0xE1;

    private JpegParser() {
    }

    static ImageInfo parse(BinaryFile file) throws IOException {
        if (file.u16BE() != (0xFF00 | SOI)) {
            throw new FormatException("JPEG: неверная сигнатура SOI");
        }
        Frame frame = null;
        TiffParser.Dpi jfif = new TiffParser.Dpi(null, null);
        TiffParser.Dpi exif = new TiffParser.Dpi(null, null);
        boolean scanFound = false;
        boolean inScan = false;
        while (file.position() < file.length()) {
            int marker = inScan ? nextScanMarker(file) : nextMarker(file);
            inScan = false;
            if (marker == EOI) {
                if (frame == null || !scanFound || frame.height() == 0) {
                    throw new FormatException("JPEG: нет полного кадра перед EOI");
                }
                return image(frame, jfif, exif);
            }
            if (marker == SOI || isRestart(marker)) {
                throw new FormatException("JPEG: маркер вне допустимой позиции");
            }
            if (marker == 0x01) {
                continue; // TEM — одиночный маркер без длины.
            }

            int segmentSize = file.u16BE();
            if (segmentSize < 2) {
                throw new FormatException("JPEG: неверная длина сегмента");
            }
            long start = file.position();
            int payloadSize = segmentSize - 2;
            file.require(start, payloadSize);
            long end = start + payloadSize;

            if (isFrame(marker)) {
                frame = readFrame(file, marker, payloadSize);
            } else if (marker == APP0 && payloadSize >= 5 && file.ascii(5).equals("JFIF\0")) {
                jfif = readJfifDpi(file, payloadSize);
            } else if (marker == APP1 && payloadSize >= 6 && file.ascii(6).equals("Exif\0\0")) {
                exif = TiffParser.readExifDpi(file, start + 6, end);
            } else if (marker == DNL && scanFound && frame != null && payloadSize == 2) {
                int lines = file.u16BE();
                if (lines == 0 || (frame.height() != 0 && frame.height() != lines)) {
                    throw new FormatException("JPEG: неверная высота в маркере DNL");
                }
                frame = new Frame(frame.width(), lines, frame.precision(), frame.components(),
                        frame.componentIds(), frame.marker());
            } else if (marker == SOS) {
                if (frame == null || payloadSize < 6) {
                    throw new FormatException("JPEG: неверный заголовок скана SOS");
                }
                int components = file.u8();
                if (components < 1 || components > Math.min(4, frame.components())
                        || payloadSize != 4 + 2 * components) {
                    throw new FormatException("JPEG: неверная длина скана SOS");
                }
                boolean[] used = new boolean[256];
                for (int i = 0; i < components; i++) {
                    int id = file.u8();
                    file.skip(1); // Номера таблиц энтропийного кодирования.
                    if (!frame.componentIds()[id] || used[id]) {
                        throw new FormatException("JPEG: неверный идентификатор компоненты в SOS");
                    }
                    used[id] = true;
                }
                // Обычно EOI — последние два байта. Сжатые данные тогда читать не нужно.
                if (!scanFound && frame.height() != 0 && endsWithEoi(file, end)) {
                    return image(frame, jfif, exif);
                }
                scanFound = true;
                inScan = true;
            }
            file.seek(end);
        }
        throw new FormatException("JPEG: отсутствует завершающий маркер EOI");
    }

    private static boolean endsWithEoi(BinaryFile file, long scanStart) throws IOException {
        long last = file.length() - 2;
        if (last <= scanStart) {
            return false;
        }
        file.seek(last);
        return file.u16BE() == (0xFF00 | EOI);
    }

    private static int nextMarker(BinaryFile file) throws IOException {
        if (file.u8() != 0xFF) {
            throw new FormatException("JPEG: ожидался маркер FF");
        }
        int marker;
        do {
            marker = file.u8();
        } while (marker == 0xFF);
        if (marker == 0) {
            throw new FormatException("JPEG: байты FF 00 вне сжатого скана");
        }
        return marker;
    }

    private static int nextScanMarker(BinaryFile file) throws IOException {
        boolean prefix = false;
        while (file.position() < file.length()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
            long start = file.position();
            byte[] block = file.bytes((int) Math.min(16 * 1024, file.length() - start));
            for (int index = 0; index < block.length; index++) {
                int value = block[index] & 0xFF;
                if (value == 0xFF) {
                    prefix = true;
                } else if (prefix) {
                    prefix = false;
                    // FF 00 — экранированный байт данных. RSTn не завершает скан.
                    if (value != 0 && value != 0x01 && !isRestart(value)) {
                        file.seek(start + index + 1);
                        return value;
                    }
                }
            }
        }
        throw new FormatException("JPEG: отсутствует завершающий маркер EOI");
    }

    private static Frame readFrame(BinaryFile file, int marker, int payloadSize) throws IOException {
        if (payloadSize < 9) {
            throw new FormatException("JPEG: неполный заголовок кадра SOF");
        }
        int precision = file.u8();
        int height = file.u16BE();
        int width = file.u16BE();
        int components = file.u8();
        boolean validPrecision = switch (marker & 3) {
            case 0 -> precision == 8;
            case 3 -> precision >= 2 && precision <= 16;
            default -> precision == 8 || precision == 12;
        };
        if (width == 0 || !validPrecision || components == 0 || payloadSize != 6 + 3 * components) {
            throw new FormatException("JPEG: неверная геометрия или компоненты кадра SOF");
        }
        boolean[] ids = new boolean[256];
        for (int i = 0; i < components; i++) {
            int id = file.u8();
            int sampling = file.u8();
            int quantization = file.u8();
            int horizontal = sampling >>> 4;
            int vertical = sampling & 15;
            if (ids[id] || horizontal < 1 || horizontal > 4 || vertical < 1 || vertical > 4
                    || quantization > 3 || ((marker & 3) == 3 && quantization != 0)) {
                throw new FormatException("JPEG: неверное описание компоненты в SOF");
            }
            ids[id] = true;
        }
        return new Frame(width, height, precision, components, ids, marker);
    }

    private static TiffParser.Dpi readJfifDpi(BinaryFile file, int payloadSize) throws IOException {
        if (payloadSize < 14) {
            throw new FormatException("JPEG: неполный заголовок JFIF");
        }
        file.skip(2); // Версия JFIF.
        int unit = file.u8();
        int x = file.u16BE();
        int y = file.u16BE();
        int thumbnailWidth = file.u8();
        int thumbnailHeight = file.u8();
        if (payloadSize < 14 + 3 * thumbnailWidth * thumbnailHeight || unit > 2) {
            throw new FormatException("JPEG: повреждён сегмент JFIF");
        }
        double factor = unit == 2 ? 2.54 : 1.0;
        return new TiffParser.Dpi(unit == 0 || x == 0 ? null : x * factor,
                unit == 0 || y == 0 ? null : y * factor);
    }

    private static ImageInfo image(Frame frame, TiffParser.Dpi jfif, TiffParser.Dpi exif) {
        String mode = switch (frame.marker() & 3) {
            case 0 -> "baseline DCT";
            case 1 -> "sequential DCT";
            case 2 -> "progressive DCT";
            default -> "без потерь";
        };
        String coding = (frame.marker() & 8) == 0 ? "Хаффман" : "арифметическое";
        Double x = jfif.x() != null ? jfif.x() : exif.x();
        Double y = jfif.y() != null ? jfif.y() : exif.y();
        return new ImageInfo("JPEG", frame.width(), frame.height(), x, y,
                frame.precision() * frame.components(), mode + ", " + coding, null);
    }

    private static boolean isRestart(int marker) {
        return marker >= 0xD0 && marker <= 0xD7;
    }

    private static boolean isFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF
                && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    private record Frame(int width, int height, int precision, int components, boolean[] componentIds, int marker) {
    }
}
