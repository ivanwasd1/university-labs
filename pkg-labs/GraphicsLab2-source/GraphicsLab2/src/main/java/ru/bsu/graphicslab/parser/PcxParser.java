package ru.bsu.graphicslab.parser;

import java.io.IOException;

final class PcxParser {
    private static final int HEADER_SIZE = 128;
    private static final int PALETTE_SIZE = 1 + 256 * 3;

    static ImageInfo parse(BinaryFile f) throws IOException {
        f.require(0, HEADER_SIZE);
        if (f.u8() != 0x0A) {
            throw new FormatException("Нет сигнатуры PCX");
        }
        int version = f.u8();
        int encoding = f.u8();
        int bitsPerPlane = f.u8();
        if (version != 0 && (version < 2 || version > 5)) {
            throw new FormatException("Неизвестная версия PCX: " + version);
        }
        if (encoding != 0 && encoding != 1) {
            throw new FormatException("Неизвестный способ кодирования PCX: " + encoding);
        }
        if (bitsPerPlane != 1 && bitsPerPlane != 2 && bitsPerPlane != 4 && bitsPerPlane != 8) {
            throw new FormatException("Некорректная глубина цветовой плоскости PCX");
        }

        int xMin = f.u16LE();
        int yMin = f.u16LE();
        int xMax = f.u16LE();
        int yMax = f.u16LE();
        int width = xMax - xMin + 1;
        int height = yMax - yMin + 1;
        int dpiX = f.u16LE();
        int dpiY = f.u16LE();
        f.seek(65);
        int planes = f.u8();
        int bytesPerLine = f.u16LE();
        if (width <= 0 || height <= 0 || planes < 1 || planes > 4
                || (bytesPerLine & 1) != 0 || bytesPerLine < (width * bitsPerPlane + 7) / 8) {
            throw new FormatException("Некорректные размеры строки или число плоскостей PCX");
        }

        int depth = bitsPerPlane * planes;
        String details = null;
        long dataEnd = f.length();
        if (version == 5 && depth == 8 && planes == 1 && dataEnd >= HEADER_SIZE + PALETTE_SIZE + 1) {
            f.seek(dataEnd - PALETTE_SIZE);
            if (f.u8() == 0x0C) {
                dataEnd -= PALETTE_SIZE;
                details = "Палитра: 256 цветов";
            }
        } else if (depth <= 4 && version != 0 && version != 3) {
            details = "Палитра: до " + (1 << depth) + " цветов";
        }

        long rowBytes = (long) planes * bytesPerLine;
        // RLE-пара занимает два байта и описывает не более 63 байтов одной строки.
        long minStoredRow = encoding == 0 ? rowBytes
                : 2 * (rowBytes / 63) + Math.min(2, rowBytes % 63);
        if (dataEnd - HEADER_SIZE < minStoredRow * height) {
            throw new FormatException("Не хватает данных для всех строк PCX");
        }

        return new ImageInfo("PCX", width, height, dpiX == 0 ? null : (double) dpiX,
                dpiY == 0 ? null : (double) dpiY, depth,
                encoding == 1 ? "RLE" : "Без сжатия", details);
    }
}
