package ru.university.colormodels.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ru.university.colormodels.controller.ColorController;

class ColorConverterTest {

    @Test
    void redConvertsToXyzLabAndCmyk() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        double[] red = {255.0, 0.0, 0.0};

        assertArrayEquals(new double[] {41.24, 21.26, 1.93},
                converter.fromRgb(ColorModelType.XYZ, red, CmykAlgorithm.GCR), 0.03);
        assertArrayEquals(new double[] {53.24, 80.09, 67.20},
                converter.fromRgb(ColorModelType.LAB, red, CmykAlgorithm.GCR), 0.08);
        assertArrayEquals(new double[] {0.0, 100.0, 100.0, 0.0},
                converter.fromRgb(ColorModelType.CMYK, red, CmykAlgorithm.GCR), 1e-9);
        assertArrayEquals(new double[] {0.0, 100.0, 100.0, 0.0},
                converter.fromRgb(ColorModelType.CMYK, red, CmykAlgorithm.UCR), 1e-9);
    }

    @Test
    void whiteUsesSelectedIlluminant() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        double[] white = {255.0, 255.0, 255.0};

        for (Illuminant illuminant : Illuminant.values()) {
            converter.setIlluminant(illuminant);
            assertArrayEquals(illuminant.referenceWhite(),
                    converter.fromRgb(ColorModelType.XYZ, white, CmykAlgorithm.GCR), 1e-8);
            assertArrayEquals(new double[] {100.0, 0.0, 0.0},
                    converter.fromRgb(ColorModelType.LAB, white, CmykAlgorithm.GCR), 1e-8);
        }
    }

    @Test
    void matrixChangesWithIlluminant() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        long revision = converter.getMatrixRevision();
        double coefficient = converter.getRgbToXyzMatrixValue(0, 0);

        converter.setIlluminant(Illuminant.D50);

        assertEquals(revision + 1, converter.getMatrixRevision());
        assertNotEquals(coefficient, converter.getRgbToXyzMatrixValue(0, 0), 1e-4);
    }

    @Test
    void xyzAndLabConvertBackToRgb() {
        double[] rgb = {12.0, 34.0, 56.0};

        for (Illuminant illuminant : Illuminant.values()) {
            ColorConverter converter = new ColorConverter(illuminant);
            for (ColorModelType model : new ColorModelType[] {
                    ColorModelType.XYZ, ColorModelType.LAB}) {
                double[] values = converter.fromRgb(model, rgb, CmykAlgorithm.GCR);
                ConversionOutcome outcome = converter.toRgb(
                        model, values, CmykAlgorithm.GCR, GamutStrategy.CLIPPING);
                assertArrayEquals(rgb, outcome.rgb(), 1e-6);
                assertFalse(outcome.outOfGamut());
            }
        }
    }

    @Test
    void xyzAndLabStayExactOutsideRgbGamut() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        ColorController controller = new ColorController(converter);
        double[] xyz = {0.0, 0.0, 5.0};

        controller.updateFrom(ColorModelType.XYZ, xyz);
        assertArrayEquals(xyz, controller.valuesFor(ColorModelType.XYZ), 1e-12);
        assertArrayEquals(converter.xyzToLab(xyz),
                controller.valuesFor(ColorModelType.LAB), 1e-10);
        assertTrue(controller.hasWarning());

        controller.setGamutStrategy(GamutStrategy.SCALING);
        assertArrayEquals(xyz, controller.valuesFor(ColorModelType.XYZ), 1e-12);
        assertArrayEquals(converter.xyzToLab(xyz),
                controller.valuesFor(ColorModelType.LAB), 1e-10);
    }

    @Test
    void hsvAndHlsConvertBackToRgb() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        double[] rgb = {12.0, 34.0, 56.0};

        assertArrayEquals(new double[] {210.0, 78.5714286, 21.9607843},
                converter.fromRgb(ColorModelType.HSV, rgb, CmykAlgorithm.GCR), 1e-6);
        assertArrayEquals(new double[] {210.0, 13.3333333, 64.7058824},
                converter.fromRgb(ColorModelType.HLS, rgb, CmykAlgorithm.GCR), 1e-6);

        for (ColorModelType model : new ColorModelType[] {
                ColorModelType.HSV, ColorModelType.HLS}) {
            double[] values = converter.fromRgb(model, rgb, CmykAlgorithm.GCR);
            assertArrayEquals(rgb, converter.toRgb(
                    model, values, CmykAlgorithm.GCR, GamutStrategy.CLIPPING).rgb(), 1e-7);
        }
    }

    @Test
    void ucrAndGcrGiveDifferentCmykAndPreserveRgb() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        double[] rgb = {128.0, 128.0, 128.0};
        double[] ucr = converter.fromRgb(ColorModelType.CMYK, rgb, CmykAlgorithm.UCR);
        double[] gcr = converter.fromRgb(ColorModelType.CMYK, rgb, CmykAlgorithm.GCR);

        assertArrayEquals(new double[] {49.8039216, 49.8039216, 49.8039216, 0.0},
                ucr, 1e-6);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 49.8039216},
                gcr, 1e-6);
        assertTrue(gcr[3] > ucr[3]);
        assertArrayEquals(rgb, converter.cmykToRgb(ucr), 1e-7);
        assertArrayEquals(rgb, converter.cmykToRgb(gcr), 1e-7);
    }

    @Test
    void blackConvertsToZeroXyzAndLab() {
        ColorConverter converter = new ColorConverter(Illuminant.D65);
        double[] black = {0.0, 0.0, 0.0};

        assertArrayEquals(new double[] {0.0, 0.0, 0.0},
                converter.fromRgb(ColorModelType.XYZ, black, CmykAlgorithm.GCR), 1e-12);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0},
                converter.fromRgb(ColorModelType.LAB, black, CmykAlgorithm.GCR), 1e-12);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 100.0},
                converter.fromRgb(ColorModelType.CMYK, black, CmykAlgorithm.GCR), 1e-12);
    }

    @Test
    void clippingLimitsEachRgbChannel() {
        ConversionOutcome outcome = GamutMapper.map(
                new double[] {-25.5, 102.0, 306.0}, GamutStrategy.CLIPPING);

        assertTrue(outcome.outOfGamut());
        assertArrayEquals(new double[] {0.0, 102.0, 255.0}, outcome.rgb(), 1e-12);
    }

    @Test
    void scalingFitsAllRgbChannelsIntoRange() {
        ConversionOutcome outcome = GamutMapper.map(
                new double[] {-25.5, 102.0, 306.0}, GamutStrategy.SCALING);
        double middle = (102.0 + 25.5) * 255.0 / (306.0 + 25.5);

        assertTrue(outcome.outOfGamut());
        assertArrayEquals(new double[] {0.0, middle, 255.0}, outcome.rgb(), 1e-12);
    }

    @Test
    void inverseMatrixRestoresVector() {
        Matrix3 matrix = new Matrix3(new double[][] {
                {3.0, 0.0, 2.0},
                {2.0, 0.0, -2.0},
                {0.0, 1.0, 1.0}
        });
        double[] vector = {2.0, -1.0, 4.0};

        assertArrayEquals(vector, matrix.inverse().multiply(matrix.multiply(vector)), 1e-10);
    }
}
