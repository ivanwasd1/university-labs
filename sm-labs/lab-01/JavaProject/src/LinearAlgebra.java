import java.util.Arrays;

public class LinearAlgebra {
    public static void validate(double[][] a, double[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length)
            throw new IllegalArgumentException("Несогласованные размеры системы");
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || a[i].length != a.length)
                throw new IllegalArgumentException("Матрица должна быть квадратной");
            if (!Double.isFinite(b[i]))
                throw new IllegalArgumentException("Правая часть содержит NaN или бесконечность");
            for (double value : a[i])
                if (!Double.isFinite(value))
                    throw new IllegalArgumentException("Матрица содержит NaN или бесконечность");
        }
    }

    public static double[][] copy(double[][] a) {
        return Arrays.stream(a).map(double[]::clone).toArray(double[][]::new);
    }

    public static double[][] identity(int n) {
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) result[i][i] = 1;
        return result;
    }

    public static double norm(double[] x) {
        double result = 0;
        for (double value : x) result = Math.max(result, Math.abs(value));
        return result;
    }

    public static double norm(double[][] a) {
        double result = 0;
        for (double[] row : a) {
            double sum = 0;
            for (double value : row) sum += Math.abs(value);
            result = Math.max(result, sum);
        }
        return result;
    }

    public static double distance(double[] a, double[] b) {
        double result = 0;
        for (int i = 0; i < a.length; i++) result = Math.max(result, Math.abs(a[i] - b[i]));
        return result;
    }

    public static double[][] inverseResidual(double[][] inverse, double[][] a) {
        int n = a.length;
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) sum += inverse[i][k] * a[k][j];
                r[i][j] = (i == j ? 1.0 : 0.0) - sum;
            }
        }
        return r;
    }

    public static double[] rowSums(double[][] a) {
        double[] sums = new double[a.length];
        for (int i = 0; i < a.length; i++)
            for (double value : a[i]) sums[i] += Math.abs(value);
        return sums;
    }

    public static double maxElement(double[][] a) {
        double result = 0;
        for (double[] row : a)
            for (double value : row) result = Math.max(result, Math.abs(value));
        return result;
    }
}
