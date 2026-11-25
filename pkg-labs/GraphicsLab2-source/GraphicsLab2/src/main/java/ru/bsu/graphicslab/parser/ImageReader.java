package ru.bsu.graphicslab.parser;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class ImageReader {
    public record Result(Path file, String format, ImageInfo info, Status status, String reason) {
        public boolean successful() {
            return status == Status.OK || status == Status.WRONG_EXTENSION;
        }
    }

    public enum Status {
        OK("Готово"), WRONG_EXTENSION("Расширение не совпадает"),
        CORRUPT("Файл повреждён"), UNKNOWN("Формат не распознан"), IO_ERROR("Ошибка чтения"),
        UNSUPPORTED("Не поддерживается");

        private final String label;

        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private ImageReader() {
    }

    public static Result read(Path path) {
        return read(path, () -> false);
    }

    public static Result read(Path path, BooleanSupplier cancelled) {
        String extension = extension(path);
        String expected = formatOfExtension(extension);
        String actual = null;
        try (BinaryFile file = new BinaryFile(path, cancelled)) {
            if (file.length() == 0) {
                throw new FormatException("Пустой файл");
            }
            actual = detect(file);
            if (actual == null) {
                return expected == null
                        ? new Result(path, null, null, Status.UNKNOWN, "Нет сигнатуры поддерживаемого формата")
                        : new Result(path, null, null, Status.CORRUPT, "Сигнатура формата " + expected + " отсутствует");
            }
            file.seek(0);
            if (actual.equals("BigTIFF")) {
                return new Result(path, actual, null, Status.UNSUPPORTED, "Поддерживается классический TIFF 6.0");
            }
            ImageInfo info = switch (actual) {
                case "BMP" -> BmpParser.parse(file);
                case "PNG" -> PngParser.parse(file);
                case "JPEG" -> JpegParser.parse(file);
                case "GIF" -> GifParser.parse(file);
                case "TIFF" -> TiffParser.parse(file);
                case "PCX" -> PcxParser.parse(file);
                default -> throw new AssertionError(actual);
            };
            if (!actual.equals(expected)) {
                String suffix = extension.isEmpty() ? "расширение отсутствует" : "расширение ." + extension;
                return new Result(path, actual, info, Status.WRONG_EXTENSION, "Формат " + actual + ", " + suffix);
            }
            return new Result(path, actual, info, Status.OK, "");
        } catch (FormatException e) {
            return new Result(path, actual, null, Status.CORRUPT, e.getMessage());
        } catch (EOFException e) {
            return new Result(path, actual, null, Status.CORRUPT, "Неожиданный конец файла");
        } catch (FileNotFoundException | SecurityException e) {
            return new Result(path, actual, null, Status.IO_ERROR, "Не удалось открыть файл");
        } catch (IOException e) {
            return new Result(path, actual, null, Status.IO_ERROR, "Не удалось прочитать файл");
        }
    }

    private static String detect(BinaryFile file) throws IOException {
        int first = file.u8();
        if (first == 0x0a) {
            if (file.length() < 4) {
                return null;
            }
            int version = file.u8();
            int encoding = file.u8();
            int bits = file.u8();
            if ((version == 0 || version == 2 || version == 3 || version == 4 || version == 5)
                    && encoding <= 1 && (bits == 1 || bits == 2 || bits == 4 || bits == 8)) {
                return "PCX";
            }
            return null;
        }
        if (file.length() < 2) {
            return null;
        }
        int pair = first << 8 | file.u8();
        return switch (pair) {
            case 0x424d -> "BMP";
            case 0xffd8 -> "JPEG";
            case 0x8950 -> "PNG";
            case 0x4749 -> file.length() >= 3 && file.u8() == 'F' ? "GIF" : null;
            case 0x4949 -> file.length() >= 4 ? tiffFormat(file.u16LE()) : null;
            case 0x4d4d -> file.length() >= 4 ? tiffFormat(file.u16BE()) : null;
            default -> null;
        };
    }

    private static String tiffFormat(int version) {
        return switch (version) {
            case 42 -> "TIFF";
            case 43 -> "BigTIFF";
            default -> null;
        };
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String formatOfExtension(String extension) {
        return switch (extension) {
            case "bmp" -> "BMP";
            case "png" -> "PNG";
            case "jpg", "jpeg", "jpe" -> "JPEG";
            case "gif" -> "GIF";
            case "tif", "tiff" -> "TIFF";
            case "pcx" -> "PCX";
            default -> null;
        };
    }
}
