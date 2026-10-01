package ru.university.colormodels.model;

public enum GamutStrategy {
    CLIPPING("Clipping — обрезание"),
    SCALING("Scaling — масштабирование");

    private final String displayName;

    GamutStrategy(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
