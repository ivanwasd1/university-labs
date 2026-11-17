public class DirectMethods {
    public record Result(double[][] upper, double[][] rhs, double[][] solution,
                         LinearAlgebra.Operations operations) {}

    private static void validate(double[][] a, double[][] rhs) {
        if (rhs == null || rhs.length == 0 || rhs[0] == null || rhs[0].length == 0)
            throw new IllegalArgumentException("Не заданы правые части");
        LinearAlgebra.validate(a, new double[rhs.length]);
        for (double[] row : rhs) {
            if (row == null || row.length != rhs[0].length)
                throw new IllegalArgumentException("Несогласованные размеры правых частей");
            for (double v : row)
                if (!Double.isFinite(v)) throw new IllegalArgumentException("Недопустимая правая часть");
        }
    }

    private static void checkPivot(double pivot, double threshold) {
        if (!Double.isFinite(pivot) || Math.abs(pivot) <= threshold)
            throw new ArithmeticException("Нулевой или слишком малый ведущий элемент");
    }

    private static double[][] backSubstitution(double[][] u, double[][] rhs,
                                               LinearAlgebra.Operations op) {
        int n = u.length, m = rhs[0].length;
        double[][] x = new double[n][m];
        double threshold = 100 * Math.ulp(1.0) * LinearAlgebra.norm(u);
        for (int i = n - 1; i >= 0; i--) {
            checkPivot(u[i][i], threshold);
            for (int c = 0; c < m; c++) {
                double sum = 0;
                for (int j = i + 1; j < n; j++) sum = op.add(sum, op.mul(u[i][j], x[j][c]));
                x[i][c] = op.div(op.sub(rhs[i][c], sum), u[i][i]);
                if (!Double.isFinite(x[i][c])) throw new ArithmeticException("Переполнение вычислений");
            }
        }
        return x;
    }

    public static Result gauss(double[][] matrix, double[][] right) {
        validate(matrix, right);
        double[][] a = LinearAlgebra.copy(matrix), rhs = LinearAlgebra.copy(right);
        var op = new LinearAlgebra.Operations();
        int n = a.length, m = rhs[0].length;
        double threshold = 100 * Math.ulp(1.0) * LinearAlgebra.norm(a);
        for (int k = 0; k < n - 1; k++) {
            checkPivot(a[k][k], threshold);
            for (int i = k + 1; i < n; i++) {
                double factor = op.div(a[i][k], a[k][k]);
                a[i][k] = 0;
                for (int j = k + 1; j < n; j++) a[i][j] = op.sub(a[i][j], op.mul(factor, a[k][j]));
                for (int c = 0; c < m; c++) rhs[i][c] = op.sub(rhs[i][c], op.mul(factor, rhs[k][c]));
            }
        }
        return new Result(a, rhs, backSubstitution(a, rhs, op), op);
    }

    public static Result householder(double[][] matrix, double[][] right) {
        validate(matrix, right);
        double[][] a = LinearAlgebra.copy(matrix), rhs = LinearAlgebra.copy(right);
        var op = new LinearAlgebra.Operations();
        int n = a.length, m = rhs[0].length;
        for (int k = 0; k < n - 1; k++) {
            double[] v = new double[n - k];
            double squared = 0;
            for (int i = k; i < n; i++) {
                v[i - k] = a[i][k];
                squared = op.add(squared, op.mul(v[i - k], v[i - k]));
            }
            if (squared == 0 || !Double.isFinite(squared))
                throw new ArithmeticException("Нулевой столбец или недопустимый масштаб матрицы");
            double alpha = -Math.copySign(op.root(squared), v[0]);
            v[0] = op.sub(v[0], alpha);
            double vv = 0;
            for (double value : v) vv = op.add(vv, op.mul(value, value));
            if (vv == 0 || !Double.isFinite(vv)) throw new ArithmeticException("Недопустимое отражение");
            double beta = op.div(2, vv);
            for (int j = k + 1; j < n; j++) {
                double dot = 0;
                for (int i = k; i < n; i++) dot = op.add(dot, op.mul(v[i - k], a[i][j]));
                double factor = op.mul(beta, dot);
                for (int i = k; i < n; i++) a[i][j] = op.sub(a[i][j], op.mul(factor, v[i - k]));
            }
            for (int c = 0; c < m; c++) {
                double dot = 0;
                for (int i = k; i < n; i++) dot = op.add(dot, op.mul(v[i - k], rhs[i][c]));
                double factor = op.mul(beta, dot);
                for (int i = k; i < n; i++) rhs[i][c] = op.sub(rhs[i][c], op.mul(factor, v[i - k]));
            }
            a[k][k] = alpha;
            for (int i = k + 1; i < n; i++) a[i][k] = 0;
        }
        return new Result(a, rhs, backSubstitution(a, rhs, op), op);
    }
}
