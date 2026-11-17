public class GaussMethod {
    public record Result(double[][] upper, double[][] rhs, double[] x,
                         double[][] inverse, double determinant) {}

    public static Result solve(double[][] a, double[] b) {
        int n = a.length;
        double[][] u = new double[n][];
        double[][] rhs = new double[n][n + 1];

        for (int i = 0; i < n; i++) {
            u[i] = a[i].clone();
            rhs[i][0] = b[i];
            rhs[i][i + 1] = 1.0;
        }

        forwardElimination(u, rhs);
        double[][] solution = backSubstitution(u, rhs);

        double determinant = 1.0;
        double[] x = new double[n];
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            determinant *= u[i][i];
            x[i] = solution[i][0];
            for (int j = 0; j < n; j++) {
                inverse[i][j] = solution[i][j + 1];
            }
        }
        return new Result(u, rhs, x, inverse, determinant);
    }

    private static void forwardElimination(double[][] u, double[][] rhs) {
        int n = u.length;
        for (int k = 0; k < n; k++) {
            double pivot = u[k][k];
            if (pivot == 0.0) {
                throw new ArithmeticException("Нулевой ведущий элемент");
            }
            for (int i = k + 1; i < n; i++) {
                double factor = u[i][k] / pivot;
                u[i][k] = 0.0;
                for (int j = k + 1; j < n; j++) {
                    u[i][j] -= factor * u[k][j];
                }
                for (int c = 0; c < rhs[i].length; c++) {
                    rhs[i][c] -= factor * rhs[k][c];
                }
            }
        }
    }

    private static double[][] backSubstitution(double[][] u, double[][] rhs) {
        int n = u.length;
        int columns = rhs[0].length;
        double[][] solution = new double[n][columns];
        for (int c = 0; c < columns; c++) {
            for (int i = n - 1; i >= 0; i--) {
                double sum = 0.0;
                for (int j = i + 1; j < n; j++) {
                    sum += u[i][j] * solution[j][c];
                }
                solution[i][c] = (rhs[i][c] - sum) / u[i][i];
            }
        }
        return solution;
    }
}
