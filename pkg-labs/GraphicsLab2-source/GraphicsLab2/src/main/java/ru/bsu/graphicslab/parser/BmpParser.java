package ru.bsu.graphicslab.parser;

import java.io.IOException;

final class BmpParser {
    private static final int FILE_HEADER_SIZE = 14;
    private static final int CORE_HEADER_SIZE = 12;
    private static final int INFO_HEADER_SIZE = 40;

    static ImageInfo parse(BinaryFile f) throws IOException {
        if (!f.ascii(2).equals("BM")) {
            throw new FormatException("Нет сигнатуры BMP");
        }
        long fileSize = f.u32LE();
        if (fileSize < FILE_HEADER_SIZE || fileSize > f.length()) {
            throw new FormatException("Размер BMP в заголовке не соответствует размеру файла");
        }
        f.skip(4); // Зарезервированные поля.
        long pixelOffset = f.u32LE();
        long dibSize = f.u32LE();
        boolean core = dibSize == CORE_HEADER_SIZE;
        if (!core && dibSize != INFO_HEADER_SIZE && dibSize != 52 && dibSize != 56
                && dibSize != 108 && dibSize != 124) {
            throw new FormatException("Неподдерживаемый DIB-заголовок BMP: " + dibSize + " байт");
        }
        long paletteStart = FILE_HEADER_SIZE + dibSize;
        if (pixelOffset < paletteStart || pixelOffset >= fileSize) {
            throw new FormatException("Некорректное смещение данных BMP");
        }

        long width = core ? f.u16LE() : f.i32LE();
        long signedHeight = core ? f.u16LE() : f.i32LE();
        int planes = f.u16LE();
        int depth = f.u16LE();
        if (width <= 0 || signedHeight == 0 || planes != 1) {
            throw new FormatException("Некорректные размеры или число плоскостей BMP");
        }
        long height = Math.abs(signedHeight);
        long compressionCode = core ? 0 : f.u32LE();
        long imageSize = core ? 0 : f.u32LE();
        Double dpiX = core ? null : dpi(f.i32LE());
        Double dpiY = core ? null : dpi(f.i32LE());
        long paletteColors = core ? 0 : f.u32LE();
        String compression = compressionName(compressionCode, depth, core);

        boolean uncompressed = compressionCode == 0 || compressionCode == 3
                || compressionCode == 6 || compressionCode == 11;
        if (signedHeight < 0 && !uncompressed) {
            throw new FormatException("У сжатого BMP высота должна быть положительной");
        }
        if (compressionCode == 3 && dibSize == INFO_HEADER_SIZE) {
            paletteStart += 12; // Три маски DWORD после BITMAPINFOHEADER.
        }
        if (compressionCode == 6) {
            if (dibSize == INFO_HEADER_SIZE) {
                paletteStart += 16;
            } else if (dibSize < 56) {
                throw new FormatException("Отсутствует альфа-маска BMP");
            }
        }

        if (depth > 0 && depth <= 8) {
            long maxColors = 1L << depth;
            if (paletteColors > maxColors) {
                throw new FormatException("Размер палитры BMP превышает глубину цвета");
            }
            if (paletteColors == 0) {
                paletteColors = maxColors;
            }
        }
        int paletteEntrySize = core ? 3 : 4;
        if (paletteStart + paletteColors * paletteEntrySize > pixelOffset) {
            throw new FormatException("Палитра или цветовые маски выходят за границы BMP");
        }

        long available = fileSize - pixelOffset;
        if (imageSize > available) {
            throw new FormatException("Размер данных BMP больше доступного остатка файла");
        }
        if ((compressionCode == 1 || compressionCode == 2
                || compressionCode == 12 || compressionCode == 13) && imageSize < 2) {
            throw new FormatException("Недостаточная длина блока BMP RLE");
        }
        if (uncompressed) {
            // Каждая несжатая строка выровнена до четырёх байт.
            long stride = ((width * depth + 31) / 32) * 4;
            if (height > available / stride || (imageSize > 0 && height > imageSize / stride)) {
                throw new FormatException("Не хватает данных для всех строк BMP");
            }
        } else if (imageSize == 0) {
            throw new FormatException("Не указан размер сжатых данных BMP");
        }

        String details = paletteColors == 0 ? null : "Палитра: " + paletteColors + " цветов";
        return new ImageInfo("BMP", width, height, dpiX, dpiY, depth == 0 ? null : depth,
                compression, details);
    }

    private static Double dpi(int pixelsPerMeter) {
        return pixelsPerMeter <= 0 ? null : pixelsPerMeter * 0.0254;
    }

    private static String compressionName(long code, int depth, boolean core) throws FormatException {
        if (core && depth != 1 && depth != 4 && depth != 8 && depth != 24) {
            throw new FormatException("Недопустимая глубина цвета BITMAPCOREHEADER");
        }
        if (code > 13) {
            throw new FormatException("Неизвестный код сжатия BMP: " + code);
        }
        String name = switch ((int) code) {
            case 0 -> depth == 1 || depth == 2 || depth == 4 || depth == 8
                    || depth == 16 || depth == 24 || depth == 32 ? "BI_RGB" : null;
            case 1 -> depth == 8 ? "BI_RLE8" : null;
            case 2 -> depth == 4 ? "BI_RLE4" : null;
            case 3 -> depth == 16 || depth == 32 ? "BI_BITFIELDS" : null;
            case 4 -> "BI_JPEG";
            case 5 -> "BI_PNG";
            case 6 -> depth == 16 || depth == 32 ? "BI_ALPHABITFIELDS" : null;
            case 11 -> depth == 32 ? "BI_CMYK" : null;
            case 12 -> depth == 8 ? "BI_CMYKRLE8" : null;
            case 13 -> depth == 4 ? "BI_CMYKRLE4" : null;
            default -> throw new FormatException("Неизвестный код сжатия BMP: " + code);
        };
        if (name == null) {
            throw new FormatException("Глубина цвета BMP не подходит для кода сжатия " + code);
        }
        return name;
    }
}
