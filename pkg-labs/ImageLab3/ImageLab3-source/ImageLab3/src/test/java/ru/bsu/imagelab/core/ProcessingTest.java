package ru.bsu.imagelab.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.bsu.imagelab.core.Morphology.Operation;
import ru.bsu.imagelab.core.Morphology.Shape;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingTest {
    private final ParallelProcessor serial = new ParallelProcessor(1);

    @AfterEach
    void closePool() {
        serial.close();
    }

    @Test
    void kernelsAreNormalizedSymmetricAndWeighted() {
        assertArrayEquals(new double[]{0.2, 0.2, 0.2, 0.2, 0.2},
                SmoothingFilters.boxKernel(5), 1e-12);
        double[] weights = SmoothingFilters.gaussianKernel(5, 1);
        assertEquals(1, Arrays.stream(weights).sum(), 1e-12);
        assertEquals(weights[0], weights[4], 1e-12);
        assertEquals(weights[1], weights[3], 1e-12);
        assertTrue(weights[0] < weights[1] && weights[1] < weights[2]);
        assertArrayEquals(new double[]{1}, SmoothingFilters.gaussianKernel(1, 0.5), 1e-12);
    }

    @Test
    void bothFiltersKeepConstantArgbAtAllEdges() {
        int[] solid = new int[15];
        Arrays.fill(solid, 0x81ac350c);
        ImageBuffer source = ImageBuffer.of(3, 5, solid);
        for (Boundary edge : Boundary.values()) {
            assertArrayEquals(solid, SmoothingFilters.box(source, 11, edge, serial, null).pixels());
            assertArrayEquals(solid, SmoothingFilters.gaussian(source, 11, 2, edge, serial, null).pixels());
        }
    }

    @Test
    void boxCenterUsesAllNineSamplesAndRoundsOnlyAtTheEnd() {
        ImageBuffer impulse = gray(3, 0, 0, 0, 0, 255, 0, 0, 0, 0);
        ImageBuffer result = SmoothingFilters.box(impulse, 3, Boundary.CLAMP, serial, null);
        // 255 / 9 = 28.333...
        assertEquals(28, shade(result, 1, 1));
        // The two normalized weights produce 5 / 9, which rounds to 1.
        ImageBuffer weak = gray(3, 0, 0, 0, 0, 5, 0, 0, 0, 0);
        assertEquals(1, shade(SmoothingFilters.box(weak, 3, Boundary.CLAMP, serial, null), 1, 1));
        ImageBuffer precision = gray(3, 1, 1, 0, 1, 1, 0, 0, 0, 0);
        // Two rows would round 2/3 to 1 prematurely. True 4/9 rounds to zero.
        assertEquals(0, shade(SmoothingFilters.box(precision, 3, Boundary.CLAMP, serial, null), 1, 1));
    }

    @Test
    void gaussianImpulseHasExpectedCenterNeighborAndCorner() {
        ImageBuffer impulse = gray(3, 0, 0, 0, 0, 255, 0, 0, 0, 0);
        ImageBuffer result = SmoothingFilters.gaussian(impulse, 3, 1, Boundary.CLAMP, serial, null);
        // Normalized 1-D weights are approximately .2741, .4519, .2741.
        assertEquals(52, shade(result, 1, 1));
        assertEquals(32, shade(result, 0, 1));
        assertEquals(19, shade(result, 0, 0));
    }

    @Test
    void separableMatchesIndependentTwoDimensionalConvolution() {
        for (int[] shape : new int[][]{{9, 7}, {1, 6}, {8, 1}}) {
            ImageBuffer input = randomArgb(shape[0], shape[1]);
            for (Boundary edge : Boundary.values()) {
                for (int size : new int[]{3, 9}) {
                    assertChannelsClose(ReferenceConvolution.apply(input, size, 0, edge),
                            SmoothingFilters.box(input, size, edge, serial, null).pixels());
                    assertChannelsClose(ReferenceConvolution.apply(input, size, 1.6, edge),
                            SmoothingFilters.gaussian(input, size, 1.6, edge, serial, null).pixels());
                }
            }
        }
    }

    @Test
    void clampAndReflectHaveDifferentFilteredEndpoints() {
        ImageBuffer source = gray(3, 10, 40, 100);
        assertGray(SmoothingFilters.box(source, 3, Boundary.CLAMP, serial, null), 20, 50, 80);
        assertGray(SmoothingFilters.box(source, 3, Boundary.REFLECT, serial, null), 30, 50, 60);
        assertEquals(0, Boundary.CLAMP.index(-50, 4));
        assertEquals(3, Boundary.CLAMP.index(50, 4));
        assertEquals(1, Boundary.REFLECT.index(-1, 4));
        assertEquals(2, Boundary.REFLECT.index(4, 4));
        assertEquals(2, Boundary.REFLECT.index(-8, 4));
        assertEquals(0, Boundary.REFLECT.index(12, 4));
        assertEquals(0, Boundary.REFLECT.index(-999, 1));
    }

    @Test
    void colorChannelsAreFilteredIndependentlyAndCenterAlphaIsPreserved() {
        ImageBuffer source = ImageBuffer.of(3, 1, new int[]{0x11ff0000, 0x8000ff00, 0xcc0000ff});
        int[] result = SmoothingFilters.box(source, 3, Boundary.CLAMP, serial, null).pixels();
        assertArrayEquals(new int[]{0x11aa5500, 0x80555555, 0xcc0055aa}, result);
        assertArrayEquals(new int[]{0x11ff0000, 0x8000ff00, 0xcc0000ff}, source.pixels());
    }

    @Test
    void morphologyUsesMinAndMaxOfLuminance() {
        ImageBuffer source = gray(3, 101, 4, 80, 21, 102, 15, 30, 160, 35);
        assertEquals(4, shade(morph(source, Operation.EROSION, Shape.SQUARE, 3), 1, 1));
        assertEquals(160, shade(morph(source, Operation.DILATION, Shape.SQUARE, 3), 1, 1));
        assertEquals(156, shade(morph(source, Operation.GRADIENT, Shape.SQUARE, 3), 1, 1));
        ImageBuffer colors = ImageBuffer.of(3, 1, new int[]{0xffff0000, 0x8000ff00, 0xff0000ff});
        ImageBuffer eroded = morph(colors, Operation.EROSION, Shape.CROSS, 3);
        ImageBuffer dilated = morph(colors, Operation.DILATION, Shape.CROSS, 3);
        assertEquals(0x801d1d1d, eroded.pixels()[1]);
        assertEquals(0x80969696, dilated.pixels()[1]);
    }

    @Test
    void openingRemovesIsolatedPointButKeepsSquare() {
        ImageBuffer source = mask(
                "0000000",
                "0100000",
                "0000000",
                "0001110",
                "0001110",
                "0001110",
                "0000000");
        ImageBuffer expected = mask(
                "0000000",
                "0000000",
                "0000000",
                "0001110",
                "0001110",
                "0001110",
                "0000000");
        assertArrayEquals(expected.pixels(), morph(source, Operation.OPENING, Shape.SQUARE, 3).pixels());
    }

    @Test
    void closingFillsASingleDarkHole() {
        ImageBuffer source = mask("11111", "11111", "11011", "11111", "11111");
        ImageBuffer expected = mask("11111", "11111", "11111", "11111", "11111");
        assertArrayEquals(expected.pixels(), morph(source, Operation.CLOSING, Shape.SQUARE, 3).pixels());
    }

    @Test
    void hatsIsolateSmallDetailsAndRemainNonnegative() {
        ImageBuffer bright = gray(5,
                100, 100, 100, 100, 100,
                100, 100, 100, 100, 100,
                100, 100, 210, 100, 100,
                100, 100, 100, 100, 100,
                100, 100, 100, 100, 100);
        ImageBuffer dark = gray(5,
                100, 100, 100, 100, 100,
                100, 100, 100, 100, 100,
                100, 100, 15,  100, 100,
                100, 100, 100, 100, 100,
                100, 100, 100, 100, 100);
        ImageBuffer top = morph(bright, Operation.TOP_HAT, Shape.CROSS, 3);
        ImageBuffer black = morph(dark, Operation.BLACK_HAT, Shape.CROSS, 3);
        assertEquals(110, shade(top, 2, 2));
        assertEquals(85, shade(black, 2, 2));
        assertEquals(24, top.histogram()[0]);
        assertEquals(24, black.histogram()[0]);
    }

    @Test
    void structuralShapeControlsDilationFootprint() {
        ImageBuffer source = mask("0000000", "0000000", "0000000", "0001000",
                "0000000", "0000000", "0000000");
        ImageBuffer square = morph(source, Operation.DILATION, Shape.SQUARE, 5);
        ImageBuffer cross = morph(source, Operation.DILATION, Shape.CROSS, 5);
        ImageBuffer disk = morph(source, Operation.DILATION, Shape.DISK, 5);
        assertEquals(25, square.histogram()[255]);
        assertEquals(9, cross.histogram()[255]);
        assertEquals(13, disk.histogram()[255]);
        assertEquals(255, shade(disk, 4, 4));
        assertEquals(0, shade(disk, 5, 4));
    }

    @Test
    void onePixelWorksWithBothEdgesEvenWithLargeSupports() {
        ImageBuffer source = gray(1, 91);
        for (Boundary edge : Boundary.values()) {
            assertGray(SmoothingFilters.box(source, 63, edge, serial, null), 91);
            assertGray(SmoothingFilters.gaussian(source, 63, 7, edge, serial, null), 91);
            for (Operation operation : Operation.values()) {
                int expected = switch (operation) {
                    case GRADIENT, TOP_HAT, BLACK_HAT -> 0;
                    default -> 91;
                };
                assertGray(Morphology.apply(source, operation, Shape.DISK, 63, edge, serial, null), expected);
            }
        }
    }

    @Test
    void sequentialAndParallelAgreeAcrossStripBoundaries() {
        ImageBuffer source = randomArgb(265, 258);
        List<Integer> progress = new ArrayList<>();
        try (ParallelProcessor parallel = new ParallelProcessor(4)) {
            for (Boundary edge : Boundary.values()) {
                progress.clear();
                assertArrayEquals(
                        SmoothingFilters.gaussian(source, 11, 2.1, edge, serial, null).pixels(),
                        SmoothingFilters.gaussian(source, 11, 2.1, edge, parallel, progress::add).pixels());
                assertEquals(100, progress.getLast());
                for (int i = 1; i < progress.size(); i++) {
                    assertTrue(progress.get(i) >= progress.get(i - 1));
                }
                for (Operation operation : Operation.values()) {
                    assertArrayEquals(
                            Morphology.apply(source, operation, Shape.DISK, 5, edge, serial, null).pixels(),
                            Morphology.apply(source, operation, Shape.DISK, 5, edge, parallel, null).pixels(),
                            operation + " / " + edge);
                }
            }
        }
    }

    @Test
    void imageConversionAndHistogramRespectRowMajorLayout() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, 0xffff0000);
        image.setRGB(1, 0, 0xff00ff00);
        image.setRGB(0, 1, 0xff0000ff);
        image.setRGB(1, 1, 0xffffffff);
        ImageBuffer input = ImageBuffer.fromImage(image);
        assertArrayEquals(new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffffff}, input.pixels());
        assertEquals(0xff0000ff, input.toImage().getRGB(0, 1));
        int[] histogram = input.histogram();
        assertEquals(256, histogram.length);
        assertEquals(4, Arrays.stream(histogram).sum());
        for (int bin : new int[]{29, 76, 150, 255}) {
            assertEquals(1, histogram[bin]);
        }
        image.setRGB(0, 1, 0xff000000);
        assertEquals(0xff0000ff, input.pixels()[2]);
        int[] raw = {0xff000011};
        ImageBuffer copy = ImageBuffer.of(1, 1, raw);
        raw[0] = 0;
        assertEquals(0xff000011, copy.pixels()[0]);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ImageBuffer.of(0, 4, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> ImageBuffer.of(2, 2, new int[3]));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.boxKernel(0));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.boxKernel(2));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.boxKernel(65));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.gaussianKernel(3, 0));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.gaussianKernel(3, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SmoothingFilters.gaussianKernel(3, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> morph(gray(1, 17), Operation.CLOSING, Shape.DISK, 4));
    }

    private ImageBuffer morph(ImageBuffer source, Operation operation, Shape shape, int size) {
        return Morphology.apply(source, operation, shape, size, Boundary.CLAMP, serial, null);
    }

    private static ImageBuffer mask(String... rows) {
        int[] values = new int[rows.length * rows[0].length()];
        for (int i = 0; i < values.length; i++) {
            int width = rows[0].length();
            values[i] = rows[i / width].charAt(i % width) == '1' ? 255 : 0;
        }
        return gray(rows[0].length(), values);
    }

    private static ImageBuffer gray(int width, int... values) {
        int[] pixels = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            pixels[i] = 0xff000000 | values[i] * 0x010101;
        }
        return ImageBuffer.of(width, values.length / width, pixels);
    }

    private static int shade(ImageBuffer image, int x, int y) {
        return image.pixels()[y * image.width() + x] & 255;
    }

    private static void assertGray(ImageBuffer result, int... values) {
        assertArrayEquals(gray(result.width(), values).pixels(), result.pixels());
    }

    static ImageBuffer randomArgb(int width, int height) {
        Random random = new Random(1703);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xff000000 | random.nextInt(1 << 24);
        }
        return ImageBuffer.of(width, height, pixels);
    }

    static void assertChannelsClose(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i] >>> 24, actual[i] >>> 24, "Alpha at " + i);
            for (int shift : new int[]{0, 8, 16}) {
                int a = (expected[i] >>> shift) & 255;
                int b = (actual[i] >>> shift) & 255;
                assertTrue(Math.abs(a - b) <= 1, "Color at " + i + ", shift " + shift);
            }
        }
    }
}
