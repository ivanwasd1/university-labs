package ru.university.colormodels.model;

public record ConversionOutcome(
        double[] rgb,
        double[] rawRgb,
        boolean outOfGamut,
        GamutStrategy strategy) {

    public ConversionOutcome {
        rgb = rgb.clone();
        rawRgb = rawRgb.clone();
    }

    @Override
    public double[] rgb() {
        return rgb.clone();
    }

    @Override
    public double[] rawRgb() {
        return rawRgb.clone();
    }
}
