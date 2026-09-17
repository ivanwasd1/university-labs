package ru.university.colormodels.model;

/** Матрица 3x3 для преобразования RGB и XYZ. */
public final class Matrix3 {
    private final double[][] value;

    public Matrix3(double[][] value) {
        if (value.length != 3 || value[0].length != 3
                || value[1].length != 3 || value[2].length != 3) {
            throw new IllegalArgumentException("Ожидается матрица 3x3");
        }
        this.value = new double[3][3];
        for (int row = 0; row < 3; row++) {
            System.arraycopy(value[row], 0, this.value[row], 0, 3);
        }
    }

    public double[] multiply(double[] vector) {
        if (vector.length != 3) {
            throw new IllegalArgumentException("Ожидается вектор из трёх элементов");
        }
        double[] result = new double[3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                result[row] += value[row][column] * vector[column];
            }
        }
        return result;
    }

    public Matrix3 inverse() {
        double a = value[0][0];
        double b = value[0][1];
        double c = value[0][2];
        double d = value[1][0];
        double e = value[1][1];
        double f = value[1][2];
        double g = value[2][0];
        double h = value[2][1];
        double i = value[2][2];

        double determinant = a * (e * i - f * h)
                - b * (d * i - f * g)
                + c * (d * h - e * g);
        if (Math.abs(determinant) < 1e-12) {
            throw new IllegalStateException("Матрица вырождена");
        }

        double[][] inverse = {
                {e * i - f * h, c * h - b * i, b * f - c * e},
                {f * g - d * i, a * i - c * g, c * d - a * f},
                {d * h - e * g, b * g - a * h, a * e - b * d}
        };
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                inverse[row][column] /= determinant;
            }
        }
        return new Matrix3(inverse);
    }

    public double get(int row, int column) {
        return value[row][column];
    }
}
