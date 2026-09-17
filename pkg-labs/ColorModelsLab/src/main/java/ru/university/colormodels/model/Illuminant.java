package ru.university.colormodels.model;

/** Стандартные источники света. Хранятся только их координаты цветности. */
public enum Illuminant {
    D65("D65 — дневной свет", 0.31270, 0.32900),
    D50("D50 — тёплый дневной свет", 0.34567, 0.35850),
    E("E — равноэнергетический", 1.0 / 3.0, 1.0 / 3.0);

    private final String displayName;
    private final double x;
    private final double y;

    Illuminant(String displayName, double x, double y) {
        this.displayName = displayName;
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Возвращает Xn, Yn, Zn при Yn = 100. */
    public double[] referenceWhite() {
        return new double[] {
                100.0 * x / y,
                100.0,
                100.0 * (1.0 - x - y) / y
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
