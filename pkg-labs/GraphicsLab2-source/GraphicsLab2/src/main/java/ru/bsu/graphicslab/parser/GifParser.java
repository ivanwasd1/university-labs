package ru.bsu.graphicslab.parser;

import java.io.IOException;

final class GifParser {
    static ImageInfo parse(BinaryFile f) throws IOException {
        String signature = f.ascii(6);
        if (!signature.equals("GIF87a") && !signature.equals("GIF89a")) {
            throw new FormatException("Неверная сигнатура GIF");
        }
        int width = f.u16LE();
        int height = f.u16LE();
        int flags = f.u8();
        f.skip(2); // Индекс фона и отношение сторон пикселя.
        if (width == 0 || height == 0) {
            throw new FormatException("Недопустимый размер экрана GIF");
        }
        int globalDepth = readPalette(f, flags);
        Integer depth = null;
        boolean hasImage = false;
        boolean hasMissingPalette = false;
        while (f.position() < f.length()) {
            int block = f.u8();
            switch (block) {
                case 0x2C -> {
                    int left = f.u16LE();
                    int top = f.u16LE();
                    int frameWidth = f.u16LE();
                    int frameHeight = f.u16LE();
                    if (frameWidth == 0 || frameHeight == 0
                            || left + frameWidth > width || top + frameHeight > height) {
                        throw new FormatException("Кадр GIF выходит за пределы экрана");
                    }
                    int localDepth = readPalette(f, f.u8());
                    int activeDepth = localDepth == 0 ? globalDepth : localDepth;
                    if (activeDepth == 0) {
                        hasMissingPalette = true;
                    } else {
                        depth = depth == null ? activeDepth : Math.max(depth, activeDepth);
                    }
                    int codeSize = f.u8();
                    if (codeSize < 2 || codeSize > 8 || !skipSubBlocks(f)) {
                        throw new FormatException("Некорректные блоки данных LZW");
                    }
                    hasImage = true;
                }
                case 0x21 -> skipExtension(f);
                case 0x3B -> {
                    if (!hasImage) {
                        throw new FormatException("Нет растрового кадра GIF");
                    }
                    if (f.position() != f.length()) {
                        throw new FormatException("Лишние данные после завершения GIF");
                    }
                    if (hasMissingPalette) {
                        depth = null;
                    }
                    String details = depth == null ? null
                            : "Палитра до " + (1 << depth) + " цв.";
                    return new ImageInfo("GIF", width, height, null, null, depth, "LZW", details);
                }
                default -> throw new FormatException("Неизвестный блок GIF");
            }
        }
        throw new FormatException("Отсутствует завершающий байт GIF (3B)");
    }

    // Младшие биты задают размер таблицы; поле Color Resolution описывает источник.
    private static int readPalette(BinaryFile f, int flags) throws IOException {
        if ((flags & 0x80) == 0) {
            return 0;
        }
        int depth = (flags & 7) + 1;
        f.skip(3L * (1 << depth));
        return depth;
    }

    private static void skipExtension(BinaryFile f) throws IOException {
        int label = f.u8();
        if (label == 0xF9) {
            if (f.u8() != 4) {
                throw new FormatException("Неверная длина Graphic Control Extension");
            }
            f.skip(4);
            if (f.u8() != 0) {
                throw new FormatException("Нет терминатора Graphic Control Extension");
            }
            return;
        }
        if (label == 0x01 || label == 0xFF) {
            int fixedLength = label == 0x01 ? 12 : 11;
            if (f.u8() != fixedLength) {
                throw new FormatException("Неверная длина расширения GIF");
            }
            f.skip(fixedLength);
        }
        skipSubBlocks(f);
    }

    private static boolean skipSubBlocks(BinaryFile f) throws IOException {
        int size = f.u8();
        boolean hasData = size != 0;
        while (size != 0) {
            f.skip(size);
            size = f.u8();
        }
        return hasData;
    }
}
