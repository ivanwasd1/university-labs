package ru.bsu.imagelab.core;

final class ReferenceConvolution {
    private ReferenceConvolution() {
    }

    static int[] apply(ImageBuffer input, int size, double sigma, Boundary boundary) {
        double[][] weights = new double[size][size];
        int radius = size / 2;
        double sum = 0;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double weight = sigma == 0 ? 1 : Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                weights[y + radius][x + radius] = weight;
                sum += weight;
            }
        }
        for (double[] row : weights) {
            for (int x = 0; x < size; x++) {
                row[x] /= sum;
            }
        }

        int width = input.width();
        int height = input.height();
        int[][] xs = neighborhood(width, size, boundary);
        int[][] ys = neighborhood(height, size, boundary);
        int[] source = input.pixels();
        int[] result = new int[source.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double r = 0, g = 0, b = 0;
                for (int ky = 0; ky < size; ky++) {
                    int row = ys[y][ky] * width;
                    for (int kx = 0; kx < size; kx++) {
                        int argb = source[row + xs[x][kx]];
                        double weight = weights[ky][kx];
                        r += ((argb >>> 16) & 255) * weight;
                        g += ((argb >>> 8) & 255) * weight;
                        b += (argb & 255) * weight;
                    }
                }
                int index = y * width + x;
                result[index] = (source[index] & 0xff000000)
                        | ((int) Math.round(r) << 16) | ((int) Math.round(g) << 8)
                        | (int) Math.round(b);
            }
        }
        return result;
    }

    private static int[][] neighborhood(int length, int size, Boundary boundary) {
        int[][] map = new int[length][size];
        for (int position = 0; position < length; position++) {
            for (int tap = 0; tap < size; tap++) {
                int coordinate = position + tap - size / 2;
                if (boundary == Boundary.CLAMP || length == 1) {
                    coordinate = Math.max(0, Math.min(length - 1, coordinate));
                } else {
                    while (coordinate < 0 || coordinate >= length) {
                        coordinate = coordinate < 0 ? -coordinate : 2 * (length - 1) - coordinate;
                    }
                }
                map[position][tap] = coordinate;
            }
        }
        return map;
    }
}
