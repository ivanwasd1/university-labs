package ru.university.colormodels.model;

import java.util.Arrays;

/** Преобразования между цветовыми моделями. */
public final class ColorConverter {
    private static final double[][] SRGB_PRIMARIES = {
            {0.6400, 0.3300},
            {0.3000, 0.6000},
            {0.1500, 0.0600}
    };
    private static final double LAB_DELTA = 6.0 / 29.0;
    private static final double UCR_SHADOW_START = 0.5;

    private Illuminant illuminant;
    private Matrix3 rgbToXyzMatrix;
    private Matrix3 xyzToRgbMatrix;
    private long matrixRevision;

    public ColorConverter(Illuminant illuminant) {
        setIlluminant(illuminant);
    }

    public Illuminant getIlluminant() {
        return illuminant;
    }

    /** Пересчёт матриц для выбранного источника света. */
    public void setIlluminant(Illuminant illuminant) {
        if (illuminant == null) {
            throw new IllegalArgumentException("Источник света не задан");
        }
        this.illuminant = illuminant;
        this.rgbToXyzMatrix = buildRgbToXyzMatrix(illuminant);
        this.xyzToRgbMatrix = rgbToXyzMatrix.inverse();
        matrixRevision++;
    }

    public long getMatrixRevision() {
        return matrixRevision;
    }

    public double getRgbToXyzMatrixValue(int row, int column) {
        return rgbToXyzMatrix.get(row, column);
    }

    public double[] fromRgb(ColorModelType target, double[] rgb, CmykAlgorithm cmykAlgorithm) {
        requireLength(rgb, 3, "RGB");
        double[] safeRgb = {
                clamp(rgb[0], 0.0, 255.0),
                clamp(rgb[1], 0.0, 255.0),
                clamp(rgb[2], 0.0, 255.0)
        };
        return switch (target) {
            case RGB -> safeRgb;
            case CMYK -> rgbToCmyk(safeRgb, cmykAlgorithm);
            case HSV -> rgbToHsv(safeRgb);
            case HLS -> rgbToHls(safeRgb);
            case XYZ -> rgbToXyz(safeRgb);
            case LAB -> xyzToLab(rgbToXyz(safeRgb));
        };
    }

    public ConversionOutcome toRgb(
            ColorModelType source,
            double[] values,
            CmykAlgorithm cmykAlgorithm,
            GamutStrategy gamutStrategy) {
        requireLength(values, source.channelCount(), source.toString());
        double[] rawRgb = switch (source) {
            case RGB -> Arrays.copyOf(values, 3);
            case CMYK -> cmykToRgb(values);
            case HSV -> hsvToRgb(values);
            case HLS -> hlsToRgb(values);
            case XYZ -> xyzToRgbRaw(values);
            case LAB -> xyzToRgbRaw(labToXyz(values));
        };
        return GamutMapper.map(rawRgb, gamutStrategy);
    }

    public double[] rgbToXyz(double[] rgb) {
        requireLength(rgb, 3, "RGB");
        double[] linear = new double[3];
        for (int i = 0; i < 3; i++) {
            linear[i] = decodeSrgb(clamp(rgb[i], 0.0, 255.0) / 255.0);
        }
        double[] xyz = rgbToXyzMatrix.multiply(linear);
        for (int i = 0; i < xyz.length; i++) {
            xyz[i] *= 100.0;
        }
        return xyz;
    }

    public double[] xyzToRgbRaw(double[] xyz) {
        requireLength(xyz, 3, "XYZ");
        double[] normalized = {xyz[0] / 100.0, xyz[1] / 100.0, xyz[2] / 100.0};
        double[] linearRgb = xyzToRgbMatrix.multiply(normalized);
        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            rgb[i] = 255.0 * encodeSrgb(linearRgb[i]);
        }
        return rgb;
    }

    public double[] xyzToLab(double[] xyz) {
        requireLength(xyz, 3, "XYZ");
        double[] white = illuminant.referenceWhite();
        double fx = labForward(xyz[0] / white[0]);
        double fy = labForward(xyz[1] / white[1]);
        double fz = labForward(xyz[2] / white[2]);
        return new double[] {
                116.0 * fy - 16.0,
                500.0 * (fx - fy),
                200.0 * (fy - fz)
        };
    }

    public double[] labToXyz(double[] lab) {
        requireLength(lab, 3, "LAB");
        double fy = (lab[0] + 16.0) / 116.0;
        double fx = fy + lab[1] / 500.0;
        double fz = fy - lab[2] / 200.0;
        double[] white = illuminant.referenceWhite();
        return new double[] {
                white[0] * labInverse(fx),
                white[1] * labInverse(fy),
                white[2] * labInverse(fz)
        };
    }

    public double[] rgbToCmyk(double[] rgb, CmykAlgorithm algorithm) {
        requireLength(rgb, 3, "RGB");
        double cyan = 1.0 - clamp(rgb[0], 0.0, 255.0) / 255.0;
        double magenta = 1.0 - clamp(rgb[1], 0.0, 255.0) / 255.0;
        double yellow = 1.0 - clamp(rgb[2], 0.0, 255.0) / 255.0;
        double gray = Math.min(cyan, Math.min(magenta, yellow));

        double black;
        if (algorithm == CmykAlgorithm.GCR) {
            black = gray;
        } else if (gray <= UCR_SHADOW_START) {
            black = 0.0;
        } else {
            black = (gray - UCR_SHADOW_START) / (1.0 - UCR_SHADOW_START);
        }
        black = Math.min(black, gray);

        if (black >= 1.0 - 1e-12) {
            return new double[] {0.0, 0.0, 0.0, 100.0};
        }
        double denominator = 1.0 - black;
        return new double[] {
                100.0 * (cyan - black) / denominator,
                100.0 * (magenta - black) / denominator,
                100.0 * (yellow - black) / denominator,
                100.0 * black
        };
    }

    public double[] cmykToRgb(double[] cmyk) {
        requireLength(cmyk, 4, "CMYK");
        double c = clamp(cmyk[0] / 100.0, 0.0, 1.0);
        double m = clamp(cmyk[1] / 100.0, 0.0, 1.0);
        double y = clamp(cmyk[2] / 100.0, 0.0, 1.0);
        double k = clamp(cmyk[3] / 100.0, 0.0, 1.0);
        return new double[] {
                255.0 * (1.0 - c) * (1.0 - k),
                255.0 * (1.0 - m) * (1.0 - k),
                255.0 * (1.0 - y) * (1.0 - k)
        };
    }

    public double[] rgbToHsv(double[] rgb) {
        double[] normalized = normalizedRgb(rgb);
        double r = normalized[0];
        double g = normalized[1];
        double b = normalized[2];
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double hue = hue(r, g, b, max, delta);
        double saturation = max == 0.0 ? 0.0 : delta / max;
        return new double[] {hue, saturation * 100.0, max * 100.0};
    }

    public double[] hsvToRgb(double[] hsv) {
        requireLength(hsv, 3, "HSV");
        double hue = normalizeHue(hsv[0]);
        double saturation = clamp(hsv[1] / 100.0, 0.0, 1.0);
        double value = clamp(hsv[2] / 100.0, 0.0, 1.0);
        double chroma = value * saturation;
        double x = chroma * (1.0 - Math.abs((hue / 60.0) % 2.0 - 1.0));
        double m = value - chroma;
        return addAndScale(hue, chroma, x, m);
    }

    public double[] rgbToHls(double[] rgb) {
        double[] normalized = normalizedRgb(rgb);
        double r = normalized[0];
        double g = normalized[1];
        double b = normalized[2];
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double lightness = (max + min) / 2.0;
        double saturation = delta == 0.0
                ? 0.0
                : delta / (1.0 - Math.abs(2.0 * lightness - 1.0));
        return new double[] {
                hue(r, g, b, max, delta),
                lightness * 100.0,
                saturation * 100.0
        };
    }

    public double[] hlsToRgb(double[] hls) {
        requireLength(hls, 3, "HLS");
        double hue = normalizeHue(hls[0]);
        double lightness = clamp(hls[1] / 100.0, 0.0, 1.0);
        double saturation = clamp(hls[2] / 100.0, 0.0, 1.0);
        double chroma = (1.0 - Math.abs(2.0 * lightness - 1.0)) * saturation;
        double x = chroma * (1.0 - Math.abs((hue / 60.0) % 2.0 - 1.0));
        double m = lightness - chroma / 2.0;
        return addAndScale(hue, chroma, x, m);
    }

    private static Matrix3 buildRgbToXyzMatrix(Illuminant illuminant) {
        double[][] primaryValues = new double[3][3];
        for (int column = 0; column < 3; column++) {
            double x = SRGB_PRIMARIES[column][0];
            double y = SRGB_PRIMARIES[column][1];
            primaryValues[0][column] = x / y;
            primaryValues[1][column] = 1.0;
            primaryValues[2][column] = (1.0 - x - y) / y;
        }
        Matrix3 primaries = new Matrix3(primaryValues);
        double[] referenceWhite = illuminant.referenceWhite();
        double[] normalizedWhite = {
                referenceWhite[0] / 100.0,
                1.0,
                referenceWhite[2] / 100.0
        };
        double[] scale = primaries.inverse().multiply(normalizedWhite);

        double[][] result = new double[3][3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                result[row][column] = primaryValues[row][column] * scale[column];
            }
        }
        return new Matrix3(result);
    }

    private static double[] normalizedRgb(double[] rgb) {
        requireLength(rgb, 3, "RGB");
        return new double[] {
                clamp(rgb[0], 0.0, 255.0) / 255.0,
                clamp(rgb[1], 0.0, 255.0) / 255.0,
                clamp(rgb[2], 0.0, 255.0) / 255.0
        };
    }

    private static double hue(double r, double g, double b, double max, double delta) {
        if (delta == 0.0) {
            return 0.0;
        }
        double hue;
        if (max == r) {
            hue = 60.0 * (((g - b) / delta) % 6.0);
        } else if (max == g) {
            hue = 60.0 * ((b - r) / delta + 2.0);
        } else {
            hue = 60.0 * ((r - g) / delta + 4.0);
        }
        return hue < 0.0 ? hue + 360.0 : hue;
    }

    private static double[] addAndScale(double hue, double chroma, double x, double m) {
        double[] rgb;
        if (hue < 60.0) {
            rgb = new double[] {chroma, x, 0.0};
        } else if (hue < 120.0) {
            rgb = new double[] {x, chroma, 0.0};
        } else if (hue < 180.0) {
            rgb = new double[] {0.0, chroma, x};
        } else if (hue < 240.0) {
            rgb = new double[] {0.0, x, chroma};
        } else if (hue < 300.0) {
            rgb = new double[] {x, 0.0, chroma};
        } else {
            rgb = new double[] {chroma, 0.0, x};
        }
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = 255.0 * (rgb[i] + m);
        }
        return rgb;
    }

    private static double decodeSrgb(double value) {
        return value <= 0.04045
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double encodeSrgb(double value) {
        return value <= 0.0031308
                ? 12.92 * value
                : 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
    }

    private static double labForward(double value) {
        double threshold = LAB_DELTA * LAB_DELTA * LAB_DELTA;
        return value > threshold
                ? Math.cbrt(value)
                : value / (3.0 * LAB_DELTA * LAB_DELTA) + 4.0 / 29.0;
    }

    private static double labInverse(double value) {
        return value > LAB_DELTA
                ? value * value * value
                : 3.0 * LAB_DELTA * LAB_DELTA * (value - 4.0 / 29.0);
    }

    private static double normalizeHue(double hue) {
        double result = hue % 360.0;
        return result < 0.0 ? result + 360.0 : result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void requireLength(double[] values, int expected, String model) {
        if (values == null || values.length != expected) {
            throw new IllegalArgumentException(
                    model + " должен содержать " + expected + " компоненты");
        }
    }
}
