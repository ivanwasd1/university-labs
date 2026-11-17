import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public record SystemData(double[][] a, double[] b) {}

    public static SystemData variant12() {
        return new SystemData(new double[][]{
                {0.5757, -0.0758, 0.0152, 0.0303, 0.1061},
                {0.0788, 0.9014, 0.0000, -0.0606, 0.0606},
                {0.0455, 0.0000, 0.7242, -0.2121, 0.1212},
                {-0.0909, 0.1909, 0.0000, 0.7121, -0.0303},
                {0.3788, 0.0000, 0.1364, 0.0152, 0.8484}
        }, new double[]{3.5148, 3.8542, -4.9056, 2.3240, 0.1818});
    }

    public static SystemData parse(String text) {
        try (Scanner scanner = new Scanner(text)) {
            if (!scanner.hasNextInt()) throw new IllegalArgumentException("Ожидался порядок системы");
            int n = scanner.nextInt();
            if (n < 1 || n > 100) throw new IllegalArgumentException("Порядок системы должен быть от 1 до 100");
            double[][] a = new double[n][n];
            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= n; j++) {
                    if (!scanner.hasNext()) throw new IllegalArgumentException("Недостаточно коэффициентов");
                    double value;
                    try { value = Double.parseDouble(scanner.next().replace(',', '.')); }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("Нечисловой коэффициент"); }
                    if (j == n) b[i] = value; else a[i][j] = value;
                }
            }
            if (scanner.hasNext()) throw new IllegalArgumentException("Лишние данные после матрицы");
            LinearAlgebra.validate(a, b);
            return new SystemData(a, b);
        }
    }

    private static String json(Object value) {
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{\n");
            for (var entry : map.entrySet()) {
                if (result.length() > 2) result.append(",\n");
                result.append(json(entry.getKey().toString())).append(": ").append(json(entry.getValue()));
            }
            return result.append("\n}").toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i > 0) result.append(", ");
                result.append(json(Array.get(value, i)));
            }
            return result.append(']').toString();
        }
        throw new IllegalArgumentException("Неподдерживаемый тип результата");
    }

    private static void writeHistory(Path path, IterativeMethods.Result result,
                                     double[][] a, double[] b) throws IOException {
        StringBuilder csv = new StringBuilder("k");
        for (int i = 0; i < b.length; i++) csv.append(",x").append(i + 1);
        csv.append(",step_inf,residual_inf\n0");
        for (double value : b) csv.append(',').append(value);
        csv.append(",,").append(LinearAlgebra.norm(LinearAlgebra.residual(a, b, b))).append('\n');
        int k = 0;
        for (var step : result.history()) {
            csv.append(++k);
            for (double value : step.x()) csv.append(',').append(value);
            csv.append(',').append(step.difference()).append(',').append(step.residual()).append('\n');
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    private static void printMatrix(StringBuilder text, String title, double[][] a) {
        text.append('\n').append(title).append('\n');
        for (double[] row : a) {
            for (double value : row) text.append(String.format(Locale.US, "% .12e  ", value));
            text.append('\n');
        }
    }

    public static void calculate(SystemData data, double eps, int limit, Path directory) throws IOException {
        double[][] a = data.a();
        double[] b = data.b();
        LinearAlgebra.validate(a, b);
        int n = a.length;
        var gauss = DirectMethods.gauss(a, LinearAlgebra.column(b));
        var householder = DirectMethods.householder(a, LinearAlgebra.column(b));
        var jacobi = IterativeMethods.solve(a, b, false, eps, limit);
        var seidel = IterativeMethods.solve(a, b, true, eps, limit);
        double[][] right = new double[n][n + 1];
        for (int i = 0; i < n; i++) { right[i][0] = b[i]; right[i][i + 1] = 1; }
        var extended = DirectMethods.gauss(a, right);
        double[][] inverse = new double[n][n];
        double determinant = 1;
        for (int i = 0; i < n; i++) {
            determinant *= extended.upper()[i][i];
            System.arraycopy(extended.solution()[i], 1, inverse[i], 0, n);
        }
        double[][] inverseResidual = LinearAlgebra.inverseResidual(inverse, a);
        double[] rowSums = new double[n];
        double maximumElement = 0;
        for (int i = 0; i < n; i++) {
            for (double value : inverseResidual[i]) {
                rowSums[i] += Math.abs(value);
                maximumElement = Math.max(maximumElement, Math.abs(value));
            }
        }
        var jacobiForm = IterativeMethods.form(a, b, false);
        var seidelForm = IterativeMethods.form(a, b, true);
        double[][] solutions = {LinearAlgebra.column(gauss.solution(), 0),
                LinearAlgebra.column(householder.solution(), 0), jacobi.x(), seidel.x()};
        double[][] residuals = new double[4][];
        for (int i = 0; i < 4; i++) residuals[i] = LinearAlgebra.residual(a, solutions[i], b);
        var operations = new LinearAlgebra.Operations[]{gauss.operations(), householder.operations(),
                jacobi.operations(), seidel.operations()};
        var result = new LinkedHashMap<String, Object>();
        result.put("A", a); result.put("b", b); result.put("epsilon", eps);
        result.put("solutions", solutions); result.put("residuals", residuals);
        result.put("gauss_upper", gauss.upper()); result.put("gauss_rhs", LinearAlgebra.column(gauss.rhs(), 0));
        result.put("inverse_rhs", extended.rhs());
        result.put("householder_upper", householder.upper());
        result.put("householder_rhs", LinearAlgebra.column(householder.rhs(), 0));
        result.put("inverse", inverse); result.put("inverse_residual", inverseResidual);
        result.put("inverse_residual_row_sums", rowSums);
        result.put("inverse_residual_max_element", maximumElement);
        result.put("jacobi_B", jacobiForm.matrix()); result.put("jacobi_c", jacobiForm.constant());
        result.put("seidel_B", seidelForm.matrix()); result.put("seidel_c", seidelForm.constant());
        result.put("determinant", determinant); result.put("norm_A", LinearAlgebra.norm(a));
        result.put("norm_inverse", LinearAlgebra.norm(inverse));
        result.put("condition", LinearAlgebra.norm(a) * LinearAlgebra.norm(inverse));
        result.put("norm_inverse_residual", LinearAlgebra.norm(inverseResidual));
        result.put("q", new double[]{LinearAlgebra.norm(jacobiForm.matrix()), LinearAlgebra.norm(seidelForm.matrix())});
        result.put("iterations", new int[]{jacobi.history().size(), seidel.history().size()});
        result.put("operations", Arrays.stream(operations).map(LinearAlgebra.Operations::values).toArray(long[][]::new));
        String[] names = {"Гаусс", "Хаусхолдер", "Якоби", "Зейдель"};
        StringBuilder text = new StringBuilder("Решение СЛАУ\nНачальное приближение x0 = b\nТочность = " + eps + "\n");
        for (int i = 0; i < 4; i++) {
            text.append('\n').append(names[i]).append("\nx = ").append(Arrays.toString(solutions[i]));
            text.append("\nr = ").append(Arrays.toString(residuals[i]));
            text.append("\n||r||_inf = ").append(LinearAlgebra.norm(residuals[i]));
            text.append("\n||x-x_G||_inf = ").append(LinearAlgebra.distance(solutions[i], solutions[0]));
            text.append("\nОперации [+, *, /, sqrt] = ").append(Arrays.toString(operations[i].values())).append('\n');
        }
        text.append("\ndet(A) = ").append(determinant).append("\n||A||_inf = ").append(LinearAlgebra.norm(a));
        text.append("\n||A^-1||_inf = ").append(LinearAlgebra.norm(inverse));
        text.append("\nnu_inf(A) = ").append(result.get("condition"));
        text.append("\n||E-A^-1*A||_inf = ").append(LinearAlgebra.norm(inverseResidual));
        text.append("\nmax|Rij| = ").append(maximumElement);
        text.append("\nИтераций Якоби: ").append(jacobi.history().size());
        text.append("\nИтераций Зейделя: ").append(seidel.history().size()).append('\n');
        printMatrix(text, "Матрица U метода Гаусса", gauss.upper());
        printMatrix(text, "Преобразованные правые части [c | F]", extended.rhs());
        printMatrix(text, "Обратная матрица A^-1", inverse);
        printMatrix(text, "Матрица невязки R = E - A^-1*A", inverseResidual);
        text.append("Суммы модулей строк R: ").append(Arrays.toString(rowSums)).append('\n');
        printMatrix(text, "Матрица T метода Хаусхолдера", householder.upper());
        printMatrix(text, "Итерационная матрица Якоби", jacobiForm.matrix());
        printMatrix(text, "Итерационная матрица Зейделя", seidelForm.matrix());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("results.json"), json(result), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("results.txt"), text, StandardCharsets.UTF_8);
        writeHistory(directory.resolve("jacobi.csv"), jacobi, a, b);
        writeHistory(directory.resolve("seidel.csv"), seidel, a, b);
        System.out.print(text);
        System.out.println("Результаты сохранены в " + directory.toAbsolutePath());
    }

    public static void main(String[] args) {
        try {
            SystemData data = variant12();
            double eps = 1e-5;
            int limit = 10000;
            Path directory = Path.of("results");
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--test")) { SelfTest.run(); return; }
                if (args[i].equals("--help")) {
                    System.out.println("Без аргументов: СЛАУ-12. Параметры: --input файл --epsilon число --limit число --out папка --test");
                    return;
                }
                if (i + 1 >= args.length) throw new IllegalArgumentException("Не задано значение " + args[i]);
                String key = args[i], value = args[++i];
                switch (key) {
                    case "--input" -> data = parse(Files.readString(Path.of(value), StandardCharsets.UTF_8));
                    case "--epsilon" -> eps = Double.parseDouble(value.replace(',', '.'));
                    case "--limit" -> limit = Integer.parseInt(value);
                    case "--out" -> directory = Path.of(value);
                    default -> throw new IllegalArgumentException("Неизвестный параметр " + key);
                }
            }
            if (!(eps > 0) || !Double.isFinite(eps) || limit < 1 || limit > 100000)
                throw new IllegalArgumentException("Некорректная точность или предел итераций");
            calculate(data, eps, limit, directory);
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}
