package ru.university.colormodels.model;

/** Обработка значений RGB, вышедших за отображаемый диапазон 0..255. */
public final class GamutMapper {
    private GamutMapper() {
    }

    public static ConversionOutcome map(double[] rawRgb, GamutStrategy strategy) {
        if (rawRgb.length != 3) {
            throw new IllegalArgumentException("RGB должен содержать три компоненты");
        }

        boolean outOfGamut = false;
        for (double value : rawRgb) {
            if (value < -1e-9 || value > 255.0 + 1e-9) {
                outOfGamut = true;
                break;
            }
        }

        double[] mapped = rawRgb.clone();
        if (outOfGamut && strategy == GamutStrategy.SCALING) {
            double low = 0.0;
            double high = 255.0;
            for (double value : rawRgb) {
                low = Math.min(low, value);
                high = Math.max(high, value);
            }
            double range = high - low;
            if (range < 1e-12) {
                for (int i = 0; i < mapped.length; i++) {
                    mapped[i] = clamp(mapped[i], 0.0, 255.0);
                }
            } else {
                for (int i = 0; i < mapped.length; i++) {
                    mapped[i] = (mapped[i] - low) * 255.0 / range;
                }
            }
        } else {
            for (int i = 0; i < mapped.length; i++) {
                mapped[i] = clamp(mapped[i], 0.0, 255.0);
            }
        }
        return new ConversionOutcome(mapped, rawRgb, outOfGamut, strategy);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
