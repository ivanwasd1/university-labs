public class GaussMethod {
    public record Result(double[][] upper, double[][] rhs, double[] x,
                         double[][] inverse, double determinant) {}

    private static void checkPivot(double pivot, double threshold) {
        if (!Double.isFinite(pivot) || Math.abs(pivot) <= threshold)
            throw new ArithmeticException("Нулевой или слишком малый ведущий элемент");
    }

    private static double[][] backSubstitution(double[][] u, double[][] rhs) {
        int n = u.length, m = rhs[0].length;
        double[][] x = new double[n][m];
        double threshold = 100 * Math.ulp(1.0) * LinearAlgebra.norm(u);
        for (int i = n - 1; i >= 0; i--) {
            checkPivot(u[i][i], threshold);
            for (int c = 0; c < m; c++) {
                double sum = 0;
                for (int j = i + 1; j < n; j++) sum += u[i][j] * x[j][c];
                x[i][c] = (rhs[i][c] - sum) / u[i][i];
                if (!Double.isFinite(x[i][c]))
                    throw new ArithmeticException("Переполнение вычислений");
            }
        }
        return x;
    }

    public static Result solve(double[][] matrix, double[] b) {
        LinearAlgebra.validate(matrix, b);
        int n = matrix.length;
        double[][] a = LinearAlgebra.copy(matrix);
        double[][] rhs = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            rhs[i][0] = b[i];
            rhs[i][i + 1] = 1;
        }
        double threshold = 100 * Math.ulp(1.0) * LinearAlgebra.norm(a);
        for (int k = 0; k < n - 1; k++) {
            checkPivot(a[k][k], threshold);
            for (int i = k + 1; i < n; i++) {
                double factor = a[i][k] / a[k][k];
                a[i][k] = 0;
                for (int j = k + 1; j < n; j++)
                    a[i][j] -= factor * a[k][j];
                for (int c = 0; c <= n; c++)
                    rhs[i][c] -= factor * rhs[k][c];
            }
        }
        double[][] solution = backSubstitution(a, rhs);
        double[] x = new double[n];
        double[][] inverse = new double[n][n];
        double determinant = 1;
        for (int i = 0; i < n; i++) {
            determinant *= a[i][i];
            x[i] = solution[i][0];
            System.arraycopy(solution[i], 1, inverse[i], 0, n);
        }
        if (!Double.isFinite(determinant) || determinant == 0)
            throw new ArithmeticException("Определитель вне допустимого диапазона double");
        return new Result(a, rhs, x, inverse, determinant);
    }
}
