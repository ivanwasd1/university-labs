package ru.bsu.imagelab.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

public final class SmoothingFilters {
    private SmoothingFilters() {
    }

    public static ImageBuffer box(ImageBuffer source, int size, Boundary boundary,
                                  ParallelProcessor processor, IntConsumer progress) {
        return convolve(source, boxKernel(size), boundary, processor, progress);
    }

    public static ImageBuffer gaussian(ImageBuffer source, int size, double sigma, Boundary boundary,
                                       ParallelProcessor processor, IntConsumer progress) {
        return convolve(source, gaussianKernel(size, sigma), boundary, processor, progress);
    }

    public static double[] boxKernel(int size) {
        checkSize(size);
        double[] kernel = new double[size];
        Arrays.fill(kernel, 1.0 / size);
        return kernel;
    }

    public static double[] gaussianKernel(int size, double sigma) {
        checkSize(size);
        if (!(sigma > 0.0) || !Double.isFinite(sigma)) {
            throw new IllegalArgumentException("Сигма должна быть конечной и положительной");
        }
        int radius = size / 2;
        double[] kernel = new double[size];
        kernel[radius] = 1.0;
        double sum = 1.0;
        for (int d = 1; d <= radius; d++) {
            double distance = d / sigma;
            double weight = Math.exp(-0.5 * distance * distance);
            kernel[radius - d] = weight;
            kernel[radius + d] = weight;
            sum += 2.0 * weight;
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    private static ImageBuffer convolve(ImageBuffer source, double[] kernel, Boundary boundary,
                                        ParallelProcessor processor, IntConsumer progress) {
        Objects.requireNonNull(source, "Нет исходного изображения");
        Objects.requireNonNull(boundary, "Не выбрана обработка краёв");
        Objects.requireNonNull(processor, "Нет обработчика");
        int width = source.width();
        int height = source.height();
        int size = kernel.length;
        int radius = size / 2;
        int[] columns = boundary.indices(width, radius);
        int[] rows = boundary.indices(height, radius);
        for (int i = 0; i < rows.length; i++) {
            rows[i] *= width;
        }

        int[] input = source.pixels();
        // Между проходами сохраняются дробные значения цветовых каналов.
        double[] red = new double[input.length];
        double[] green = new double[input.length];
        double[] blue = new double[input.length];
        ImageBuffer result = new ImageBuffer(width, height);
        int[] output = result.pixels();

        if (progress != null) {
            progress.accept(0);
        }
        processor.forRows(width, height, 0, 50, progress, y -> {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                double r = 0.0;
                double g = 0.0;
                double b = 0.0;
                for (int i = 0; i < size; i++) {
                    int pixel = input[row + columns[x + i]];
                    double weight = kernel[i];
                    r += ((pixel >>> 16) & 255) * weight;
                    g += ((pixel >>> 8) & 255) * weight;
                    b += (pixel & 255) * weight;
                }
                red[row + x] = r;
                green[row + x] = g;
                blue[row + x] = b;
            }
        });

        processor.forRows(width, height, 50, 100, progress, y -> {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                double r = 0.0;
                double g = 0.0;
                double b = 0.0;
                for (int i = 0; i < size; i++) {
                    int index = rows[y + i] + x;
                    double weight = kernel[i];
                    r += red[index] * weight;
                    g += green[index] * weight;
                    b += blue[index] * weight;
                }
                int index = row + x;
                output[index] = (input[index] & 0xff000000)
                        | (rounded(r) << 16) | (rounded(g) << 8) | rounded(b);
            }
        });
        return result;
    }

    private static int rounded(double value) {
        return Math.clamp(Math.round(value), 0, 255);
    }

    static void checkSize(int size) {
        if (size < 1 || size > 63 || size % 2 == 0) {
            throw new IllegalArgumentException("Размер окна должен быть нечётным: от 1 до 63");
        }
    }
}
