package ru.bsu.graphicslab.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

final class PngParser {
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    static ImageInfo parse(BinaryFile f) throws IOException {
        if (!Arrays.equals(f.bytes(8), SIGNATURE)) {
            throw new FormatException("Неверная сигнатура PNG");
        }
        if (f.u32BE() != 13 || !f.ascii(4).equals("IHDR")) {
            throw new FormatException("Ожидается IHDR длиной 13 байт");
        }
        ByteBuffer header = readChecked(f, "IHDR", 13);
        int width = header.getInt();
        int height = header.getInt();
        int bits = Byte.toUnsignedInt(header.get());
        int color = Byte.toUnsignedInt(header.get());
        int compression = Byte.toUnsignedInt(header.get());
        int filter = Byte.toUnsignedInt(header.get());
        int interlace = Byte.toUnsignedInt(header.get());
        int depth = pixelDepth(color, bits);
        if (width <= 0 || height <= 0 || depth == 0
                || compression != 0 || filter != 0 || interlace > 1) {
            throw new FormatException("Недопустимые поля IHDR");
        }

        Double dpiX = null;
        Double dpiY = null;
        boolean hasPhysicalSize = false;
        int paletteSize = 0;
        boolean hasIdat = false;
        boolean hasImageData = false;
        boolean afterIdat = false;
        while (f.position() < f.length()) {
            if (f.length() - f.position() < 12) {
                throw new FormatException("Обрезан чанк PNG или отсутствует IEND");
            }
            long length = f.u32BE();
            String type = f.ascii(4);
            checkChunk(type, length);
            f.require(f.position(), length + 4);
            if (hasIdat && !type.equals("IDAT")) {
                afterIdat = true;
            }
            switch (type) {
                case "IHDR" -> throw new FormatException("Повторный IHDR");
                case "PLTE" -> {
                    if (hasIdat || paletteSize != 0 || color == 0 || color == 4
                            || length < 3 || length > 768 || length % 3 != 0) {
                        throw new FormatException("Некорректная палитра PLTE");
                    }
                    paletteSize = (int) (length / 3);
                    if (color == 3 && paletteSize > (1 << bits)) {
                        throw new FormatException("Палитра PLTE больше диапазона индексов");
                    }
                    readChecked(f, type, (int) length);
                }
                case "pHYs" -> {
                    if (hasIdat || hasPhysicalSize || length != 9) {
                        throw new FormatException("Некорректный чанк pHYs");
                    }
                    hasPhysicalSize = true;
                    ByteBuffer data = readChecked(f, type, 9);
                    int pixelsX = data.getInt();
                    int pixelsY = data.getInt();
                    int unit = Byte.toUnsignedInt(data.get());
                    if (pixelsX < 0 || pixelsY < 0 || unit > 1) {
                        throw new FormatException("Недопустимые поля pHYs");
                    }
                    if (unit == 1) {
                        dpiX = pixelsX > 0 ? pixelsX * 0.0254 : null;
                        dpiY = pixelsY > 0 ? pixelsY * 0.0254 : null;
                    }
                }
                case "IDAT" -> {
                    if (afterIdat || (color == 3 && paletteSize == 0)) {
                        throw new FormatException("Неверный порядок IDAT или отсутствует PLTE");
                    }
                    hasIdat = true;
                    hasImageData |= length > 0;
                    f.skip(length + 4);
                }
                case "IEND" -> {
                    if (length != 0 || !hasImageData) {
                        throw new FormatException("Некорректный IEND или отсутствуют данные IDAT");
                    }
                    readChecked(f, type, 0);
                    if (f.position() != f.length()) {
                        throw new FormatException("Лишние данные после IEND");
                    }
                    String details = "Фильтр 0 (адаптивный)";
                    if (color == 3) {
                        details += ", палитра " + paletteSize + " цв.";
                    }
                    return new ImageInfo("PNG", width, height, dpiX, dpiY, depth,
                            "DEFLATE (zlib)", details);
                }
                default -> {
                    if (Character.isUpperCase(type.charAt(0))) {
                        throw new FormatException("Неизвестный критический чанк " + type);
                    }
                    f.skip(length + 4);
                }
            }
        }
        throw new FormatException("Отсутствует IEND");
    }

    private static int pixelDepth(int color, int bits) {
        return switch (color) {
            case 0 -> bits == 1 || bits == 2 || bits == 4 || bits == 8 || bits == 16 ? bits : 0;
            case 2 -> bits == 8 || bits == 16 ? 3 * bits : 0;
            case 3 -> bits == 1 || bits == 2 || bits == 4 || bits == 8 ? bits : 0;
            case 4 -> bits == 8 || bits == 16 ? 2 * bits : 0;
            case 6 -> bits == 8 || bits == 16 ? 4 * bits : 0;
            default -> 0;
        };
    }

    private static void checkChunk(String type, long length) throws FormatException {
        if (length > Integer.MAX_VALUE) {
            throw new FormatException("Недопустимая длина чанка PNG");
        }
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')) {
                throw new FormatException("Неверный идентификатор чанка PNG");
            }
        }
    }

    // CRC включает четыре байта имени и полезные данные, но пропускает поле длины.
    private static ByteBuffer readChecked(BinaryFile f, String type, int length)
            throws IOException {
        byte[] data = f.bytes(length);
        CRC32 crc = new CRC32();
        crc.update(type.getBytes(StandardCharsets.US_ASCII));
        crc.update(data);
        if (crc.getValue() != f.u32BE()) {
            throw new FormatException("Неверная контрольная сумма " + type);
        }
        return ByteBuffer.wrap(data);
    }
}
