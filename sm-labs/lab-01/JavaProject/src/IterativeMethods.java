import java.util.ArrayList;
import java.util.List;

public class IterativeMethods {
    public record Step(double[] x, double difference, double residual) {}
    public record Result(double[] x, List<Step> history, LinearAlgebra.Operations operations) {}
    public record Form(double[][] matrix, double[] constant) {}

    public static Form form(double[][] a, double[] b, boolean seidel) {
        LinearAlgebra.validate(a, b);
        int n = a.length;
        double[][] matrix = new double[n][n];
        double[] constant = new double[n];
        for (int i = 0; i < n; i++) {
            if (a[i][i] == 0) throw new ArithmeticException("Нулевой диагональный элемент");
            for (int j = 0; j < n; j++) {
                double value = (seidel ? j > i : j != i) ? -a[i][j] : 0;
                if (seidel) for (int k = 0; k < i; k++) value -= a[i][k] * matrix[k][j];
                matrix[i][j] = value / a[i][i];
            }
            double value = b[i];
            if (seidel) for (int k = 0; k < i; k++) value -= a[i][k] * constant[k];
            constant[i] = value / a[i][i];
        }
        return new Form(matrix, constant);
    }

    public static Result solve(double[][] a, double[] b, boolean seidel, double eps, int limit) {
        LinearAlgebra.validate(a, b);
        if (!(eps > 0) || !Double.isFinite(eps) || limit < 1)
            throw new IllegalArgumentException("Точность и предел итераций должны быть положительными");
        for (int i = 0; i < a.length; i++)
            if (a[i][i] == 0) throw new ArithmeticException("Нулевой диагональный элемент");
        double[] old = b.clone();
        List<Step> history = new ArrayList<>();
        var op = new LinearAlgebra.Operations();
        for (int k = 1; k <= limit; k++) {
            double[] next = old.clone();
            for (int i = 0; i < a.length; i++) {
                double value = b[i];
                for (int j = 0; j < a.length; j++) {
                    if (j == i) continue;
                    double xj = seidel && j < i ? next[j] : old[j];
                    value = op.sub(value, op.mul(a[i][j], xj));
                }
                next[i] = op.div(value, a[i][i]);
                if (!Double.isFinite(next[i])) throw new ArithmeticException("Переполнение итераций");
            }
            double difference = 0;
            for (int i = 0; i < a.length; i++)
                difference = Math.max(difference, Math.abs(op.sub(next[i], old[i])));
            history.add(new Step(next.clone(), difference,
                    LinearAlgebra.norm(LinearAlgebra.residual(a, next, b))));
            if (difference <= eps) return new Result(next, history, op);
            old = next;
        }
        throw new ArithmeticException("Критерий остановки не выполнен за " + limit + " итераций");
    }
}
