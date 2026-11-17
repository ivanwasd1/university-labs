public class LinearAlgebra {
    public static double[][] inverseResidual(double[][] inverse, double[][] a) {
        int n = a.length;
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int k = 0; k < n; k++) {
                    sum += inverse[i][k] * a[k][j];
                }
                double identityElement = i == j ? 1.0 : 0.0;
                r[i][j] = identityElement - sum;
            }
        }
        return r;
    }

    public static double norm(double[][] matrix) {
        double maximum = 0.0;
        for (double[] row : matrix) {
            double sum = 0.0;
            for (double value : row) {
                sum += Math.abs(value);
            }
            maximum = Math.max(maximum, sum);
        }
        return maximum;
    }

    public static double maxElement(double[][] matrix) {
        double maximum = 0.0;
        for (double[] row : matrix) {
            for (double value : row) {
                maximum = Math.max(maximum, Math.abs(value));
            }
        }
        return maximum;
    }
}
