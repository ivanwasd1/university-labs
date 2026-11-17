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
        var result = GaussMethod.solve(a, b);
        require(LinearAlgebra.distance(result.x(), expected) < 1e-10);
        require(LinearAlgebra.norm(LinearAlgebra.inverseResidual(result.inverse(), a)) < 1e-12);
        require(Arrays.deepEquals(a, saved) && Arrays.equals(b, savedB));
    }

    public static void run() {
        passed = 0;
        check("Система 1 x 1", () -> known(new double[][]{{2}}, new double[]{3}));
        check("Система 2 x 2, определитель и обратная матрица", () -> {
            double[][] a = {{4, 1}, {2, 3}};
            known(a, new double[]{1, 2});
            var r = GaussMethod.solve(a, new double[]{6, 8});
            double[][] expected = {{0.3, -0.1}, {-0.2, 0.4}};
            for (int i = 0; i < 2; i++)
                require(LinearAlgebra.distance(r.inverse()[i], expected[i]) < 1e-14);
            require(r.determinant() == 10);
            require(Math.abs(LinearAlgebra.norm(a) * LinearAlgebra.norm(r.inverse()) - 3) < 1e-14);
        });
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
        check("СЛАУ-12 и независимые контрольные значения", () -> {
            var r = GaussMethod.solve(data.a(), data.b());
            double[] exact = {7.001180468429265, 3.9999353339611235,
                    -6.000272407191209, 2.9998566791649315, -2.000716438735775};
            require(LinearAlgebra.distance(r.x(), exact) < 1e-12);
            require(Math.abs(r.determinant() - 0.2099905732078905044) < 1e-14);
            double condition = LinearAlgebra.norm(data.a()) * LinearAlgebra.norm(r.inverse());
            require(Math.abs(condition - 3.484210189385931) < 1e-12);
            require(LinearAlgebra.norm(LinearAlgebra.inverseResidual(r.inverse(), data.a())) < 1e-13);
        });
        check("Вырожденная матрица", () -> fails(() ->
                GaussMethod.solve(new double[][]{{1, 2}, {2, 4}}, new double[]{1, 2})));
        check("Нулевой ведущий элемент невырожденной матрицы", () -> fails(() ->
                GaussMethod.solve(new double[][]{{0, 1}, {1, 0}}, new double[]{2, 1})));
        check("Слишком малый ведущий элемент", () -> fails(() ->
                GaussMethod.solve(new double[][]{{1e-18, 1}, {1, 1}}, new double[]{1, 2})));
        check("Норма матрицы и максимум элемента", () -> {
            double[][] a = {{0.1, -0.2}, {0.15, 0}};
            require(Math.abs(LinearAlgebra.norm(a) - 0.3) < 1e-14);
            require(LinearAlgebra.maxElement(a) == 0.2);
        });
        check("Диапазон представления определителя", () -> {
            for (double value : new double[]{1e200, 1e-200})
                fails(() -> GaussMethod.solve(new double[][]{{value, 0}, {0, value}}, new double[]{0, 0}));
        });
        check("Размеры матрицы", () -> {
            fails(() -> GaussMethod.solve(new double[0][0], new double[0]));
            fails(() -> GaussMethod.solve(new double[][]{{1, 2}}, new double[]{1}));
            fails(() -> GaussMethod.solve(new double[][]{{1}}, new double[2]));
        });
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
