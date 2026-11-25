package ru.bsu.imagelab.core;

import java.util.Objects;
import java.util.function.IntConsumer;

public final class Morphology {
    public enum Operation {
        EROSION, DILATION, OPENING, CLOSING, GRADIENT, TOP_HAT, BLACK_HAT
    }

    public enum Shape {
        SQUARE, CROSS, DISK
    }

    private Morphology() {
    }

    public static ImageBuffer apply(ImageBuffer source, Operation operation, Shape shape, int size,
                                    Boundary boundary, ParallelProcessor processor, IntConsumer progress) {
        Objects.requireNonNull(source, "Нет исходного изображения");
        Objects.requireNonNull(operation, "Не выбрана операция");
        Objects.requireNonNull(shape, "Не выбрана форма");
        Objects.requireNonNull(boundary, "Не выбрана обработка краёв");
        Objects.requireNonNull(processor, "Нет обработчика");
        SmoothingFilters.checkSize(size);
        int width = source.width();
        int height = source.height();
        int[] input = source.pixels();
        int[] gray = new int[input.length];
        Neighborhood neighborhood = neighborhood(width, height, size / 2, shape, boundary);

        if (progress != null) {
            progress.accept(0);
        }
        processor.forRows(width, height, 0, 12, progress, y -> {
            int start = y * width;
            for (int i = start; i < start + width; i++) {
                gray[i] = ImageBuffer.luminance(input[i]);
            }
        });

        int[] positive;
        int[] negative;
        switch (operation) {
            case EROSION -> {
                positive = extremum(gray, neighborhood, false, processor, progress, 12, 92);
                negative = null;
            }
            case DILATION -> {
                positive = extremum(gray, neighborhood, true, processor, progress, 12, 92);
                negative = null;
            }
            case OPENING, TOP_HAT -> {
                int[] eroded = extremum(gray, neighborhood, false, processor, progress, 12, 52);
                int[] opened = extremum(eroded, neighborhood, true, processor, progress, 52, 92);
                if (operation == Operation.TOP_HAT) {
                    positive = gray;
                    negative = opened;
                } else {
                    positive = opened;
                    negative = null;
                }
            }
            case CLOSING, BLACK_HAT -> {
                int[] dilated = extremum(gray, neighborhood, true, processor, progress, 12, 52);
                positive = extremum(dilated, neighborhood, false, processor, progress, 52, 92);
                negative = operation == Operation.BLACK_HAT ? gray : null;
            }
            case GRADIENT -> {
                positive = extremum(gray, neighborhood, true, processor, progress, 12, 52);
                negative = extremum(gray, neighborhood, false, processor, progress, 52, 92);
            }
            default -> throw new IllegalArgumentException("Неизвестная морфологическая операция");
        }

        ImageBuffer result = new ImageBuffer(width, height);
        int[] output = result.pixels();
        processor.forRows(width, height, 92, 100, progress, y -> {
            int start = y * width;
            for (int i = start; i < start + width; i++) {
                int value = negative == null ? positive[i] : Math.max(0, positive[i] - negative[i]);
                output[i] = (input[i] & 0xff000000) | (value << 16) | (value << 8) | value;
            }
        });
        return result;
    }

    private static Neighborhood neighborhood(int width, int height, int radius, Shape shape, Boundary boundary) {
        int diameter = 2 * radius + 1;
        int[] spans = new int[diameter];
        for (int i = 0; i < diameter; i++) {
            int dy = i - radius;
            // Для каждой строки элемента [−span, span] входит в его форму.
            spans[i] = switch (shape) {
                case SQUARE -> radius;
                case CROSS -> dy == 0 ? radius : 0;
                case DISK -> (int) Math.sqrt(radius * radius - dy * dy);
            };
        }
        int[] rows = boundary.indices(height, radius);
        for (int i = 0; i < rows.length; i++) {
            rows[i] *= width;
        }
        return new Neighborhood(width, height, radius, boundary.indices(width, radius), rows, spans);
    }

    private static int[] extremum(int[] gray, Neighborhood window, boolean maximum,
                                  ParallelProcessor processor, IntConsumer progress, int from, int to) {
        int width = window.width();
        int[] output = new int[gray.length];
        processor.forRows(width, window.height(), from, to, progress, y -> {
            for (int x = 0; x < width; x++) {
                int value = maximum ? 0 : 255;
                int center = x + window.radius();
                for (int i = 0; i < window.spans().length; i++) {
                    int row = window.rows()[y + i];
                    int span = window.spans()[i];
                    for (int j = center - span; j <= center + span; j++) {
                        int sample = gray[row + window.columns()[j]];
                        value = maximum ? Math.max(value, sample) : Math.min(value, sample);
                    }
                }
                output[y * width + x] = value;
            }
        });
        return output;
    }

    private record Neighborhood(int width, int height, int radius, int[] columns, int[] rows, int[] spans) {
    }
}
