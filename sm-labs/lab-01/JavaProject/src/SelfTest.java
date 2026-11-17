import java.util.Arrays;
import java.util.Random;

public class SelfTest {
    private static int passed;

    private static void check(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("OK " + name);
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("Проверка не пройдена");
    }

    private static void fails(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | ArithmeticException e) { return; }
        throw new AssertionError("Ожидалась ошибка входных данных или вычислений");
    }

    private static void known(double[][] a, double[] expected) {
        double[] b = new double[a.length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a.length; j++) b[i] += a[i][j] * expected[j];
        double[][] saved = LinearAlgebra.copy(a);
        double[] savedB = b.clone();
        var g = DirectMethods.gauss(a, LinearAlgebra.column(b));
        var h = DirectMethods.householder(a, LinearAlgebra.column(b));
        require(LinearAlgebra.distance(LinearAlgebra.column(g.solution(), 0), expected) < 1e-10);
        require(LinearAlgebra.distance(LinearAlgebra.column(h.solution(), 0), expected) < 1e-10);
        for (boolean seidel : new boolean[]{false, true}) {
            var result = IterativeMethods.solve(a, b, seidel, 1e-11, 10000);
            require(LinearAlgebra.distance(result.x(), expected) < 1e-9);
            require(result.history().get(result.history().size() - 1).difference() <= 1e-11);
            if (result.history().size() > 1)
                require(result.history().get(result.history().size() - 2).difference() > 1e-11);
        }
        var inverse = DirectMethods.gauss(a, LinearAlgebra.identity(a.length)).solution();
        require(LinearAlgebra.norm(LinearAlgebra.inverseResidual(inverse, a)) < 1e-12);
        require(Arrays.deepEquals(a, saved) && Arrays.equals(b, savedB));
    }

    public static void run() {
        passed = 0;
        check("Система 1 x 1", () -> known(new double[][]{{2}}, new double[]{3}));
        check("Система 2 x 2", () -> known(new double[][]{{4, 1}, {2, 3}}, new double[]{1, 2}));
        check("Единичная матрица", () -> known(LinearAlgebra.identity(3), new double[]{1, -2, 3}));
        check("Нулевая правая часть", () -> known(new double[][]{{3, 1}, {1, 4}}, new double[2]));
        check("Отрицательная диагональ", () -> known(new double[][]{{-4, 1}, {2, -5}}, new double[]{-1, 3}));
        check("Тридцать систем с известным решением", () -> {
            Random random = new Random(12);
            for (int test = 0; test < 30; test++) {
                int n = 2 + random.nextInt(6);
                double[][] a = new double[n][n];
                double[] x = new double[n];
                for (int i = 0; i < n; i++) {
                    x[i] = random.nextDouble() * 8 - 4;
                    double sum = 0;
                    for (int j = 0; j < n; j++) if (j != i) {
                        a[i][j] = random.nextDouble() - 0.5;
                        sum += Math.abs(a[i][j]);
                    }
                    a[i][i] = sum + 1;
                }
                known(a, x);
            }
        });
        var data = Main.variant12();
        check("СЛАУ-12 и число итераций", () -> {
            require(IterativeMethods.solve(data.a(), data.b(), false, 1e-5, 10000).history().size() == 13);
            require(IterativeMethods.solve(data.a(), data.b(), true, 1e-5, 10000).history().size() == 7);
            var g = DirectMethods.gauss(data.a(), LinearAlgebra.column(data.b()));
            require(LinearAlgebra.norm(LinearAlgebra.residual(data.a(), LinearAlgebra.column(g.solution(), 0), data.b())) < 1e-13);
        });
        check("Вырожденная матрица", () -> {
            double[][] a = {{1, 2}, {2, 4}}, rhs = {{1}, {2}};
            fails(() -> DirectMethods.gauss(a, rhs));
            fails(() -> DirectMethods.householder(a, rhs));
        });
        check("Нулевой ведущий элемент невырожденной матрицы", () -> {
            double[][] a = {{0, 1}, {1, 0}}, rhs = {{2}, {1}};
            fails(() -> DirectMethods.gauss(a, rhs));
            var h = DirectMethods.householder(a, rhs);
            require(LinearAlgebra.distance(LinearAlgebra.column(h.solution(), 0), new double[]{1, 2}) < 1e-12);
            fails(() -> IterativeMethods.solve(a, new double[]{2, 1}, false, 1e-5, 100));
        });
        check("Ограничение числа итераций", () -> fails(() -> IterativeMethods.solve(data.a(), data.b(), false, 1e-5, 1)));
        check("Расходящиеся итерации", () -> fails(() -> IterativeMethods.solve(new double[][]{{1, 2}, {2, 1}}, new double[]{1, 1}, false, 1e-5, 20)));
        check("Некорректная точность", () -> {
            for (double eps : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
                fails(() -> IterativeMethods.solve(data.a(), data.b(), false, eps, 10));
        });
        check("Размеры матрицы", () -> fails(() -> LinearAlgebra.validate(new double[][]{{1, 2}}, new double[]{1})));
        check("NaN и бесконечность", () -> {
            fails(() -> LinearAlgebra.validate(new double[][]{{Double.NaN}}, new double[]{1}));
            fails(() -> LinearAlgebra.validate(new double[][]{{1}}, new double[]{Double.POSITIVE_INFINITY}));
        });
        check("Разбор файла с десятичными запятыми", () -> {
            var parsed = Main.parse("2\n4,0 1,0 6,0\n2,0 3,0 8,0");
            require(parsed.a()[0][0] == 4 && parsed.b()[1] == 8);
        });
        check("Неполный, лишний и нечисловой ввод", () -> {
            for (String text : new String[]{"0", "101", "2 1 2", "1 2 6 9", "1 a 6", "1 NaN 6"})
                fails(() -> Main.parse(text));
        });
        System.out.println("Пройдено групп проверок: " + passed);
    }
}
