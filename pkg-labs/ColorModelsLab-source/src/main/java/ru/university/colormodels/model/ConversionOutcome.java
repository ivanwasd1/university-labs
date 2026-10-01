package ru.university.colormodels.model;

public record ConversionOutcome(
        double[] rgb,
        boolean outOfGamut) {

    public ConversionOutcome {
        rgb = rgb.clone();
    }

    @Override
    public double[] rgb() {
        return rgb.clone();
    }
}
