package ru.bsu.imagelab.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public final class ProcessingBenchmark {
    private static final int SIZE = 11;
    private static final double SIGMA = 2.2;
    private static final int WARMUPS = 3;
    private static final int TRIALS = 5;
    private static volatile int checksum;

    private ProcessingBenchmark() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        int workers = Math.min(8, Runtime.getRuntime().availableProcessors());
        System.out.printf("{\"kernel\":%d,\"sigma\":%.1f,\"warmups\":%d,"
                        + "\"trials\":%d,\"java\":\"%s\",\"os\":\"%s\","
                        + "\"processors\":%d,\"workers\":%d,\"images\":[%n",
                SIZE, SIGMA, WARMUPS, TRIALS, System.getProperty("java.version"),
                System.getProperty("os.name"), Runtime.getRuntime().availableProcessors(), workers);
        try (ParallelProcessor serial = new ParallelProcessor(1);
             ParallelProcessor parallel = new ParallelProcessor(workers)) {
            measure(input(256, 192), serial, parallel);
            System.out.println(",");
            measure(input(1280, 960), serial, parallel);
        }
        System.out.printf("%n],\"checksum\":%d}%n", checksum);
    }

    private static ImageBuffer input(int width, int height) {
        Random random = new Random(1703);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xff000000 | random.nextInt(1 << 24);
        }
        return ImageBuffer.of(width, height, pixels);
    }

    private static void measure(ImageBuffer input, ParallelProcessor serial, ParallelProcessor parallel) {
        int[] expected = run(input, serial, parallel, 0);
        int[] separated = run(input, serial, parallel, 1);
        int[] concurrent = run(input, serial, parallel, 2);
        int maxDifference = maxColorDifference(expected, separated);
        if (maxDifference > 1 || !Arrays.equals(separated, concurrent)) {
            throw new AssertionError("Benchmark algorithms disagree");
        }
        for (int round = 0; round < WARMUPS; round++) {
            for (int offset = 0; offset < 3; offset++) {
                consume(run(input, serial, parallel, (round + offset) % 3));
            }
        }

        double[][] times = new double[3][TRIALS];
        for (int trial = 0; trial < TRIALS; trial++) {
            for (int offset = 0; offset < 3; offset++) {
                int method = (trial + offset) % 3;
                long t0 = System.nanoTime();
                int[] result = run(input, serial, parallel, method);
                times[method][trial] = (System.nanoTime() - t0) / 1e6;
                consume(result);
            }
        }

        System.out.printf("{\"width\":%d,\"height\":%d,\"maxColorDifference\":%d,",
                input.width(), input.height(), maxDifference);
        System.out.printf("\"naive2dMs\":%.3f,\"separableSerialMs\":%.3f,\"separablePoolMs\":%.3f,",
                median(times[0]), median(times[1]), median(times[2]));
        System.out.printf("\"naive2dSamplesMs\":%s,\"separableSerialSamplesMs\":%s,\"separablePoolSamplesMs\":%s}",
                Arrays.toString(times[0]), Arrays.toString(times[1]), Arrays.toString(times[2]));
    }

    private static int[] run(ImageBuffer input, ParallelProcessor serial, ParallelProcessor parallel, int method) {
        return switch (method) {
            case 0 -> ReferenceConvolution.apply(input, SIZE, SIGMA, Boundary.REFLECT);
            case 1 -> SmoothingFilters.gaussian(input, SIZE, SIGMA, Boundary.REFLECT, serial, null).pixels();
            case 2 -> SmoothingFilters.gaussian(input, SIZE, SIGMA, Boundary.REFLECT, parallel, null).pixels();
            default -> throw new IllegalArgumentException();
        };
    }

    private static void consume(int[] result) {
        checksum = checksum * 31 + result[result.length / 2];
    }

    private static double median(double[] times) {
        double[] sorted = times.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static int maxColorDifference(int[] expected, int[] actual) {
        int max = 0;
        for (int i = 0; i < expected.length; i++) {
            for (int shift = 0; shift <= 16; shift += 8) {
                int a = (expected[i] >>> shift) & 255;
                int b = (actual[i] >>> shift) & 255;
                max = Math.max(max, Math.abs(a - b));
            }
        }
        return max;
    }
}
