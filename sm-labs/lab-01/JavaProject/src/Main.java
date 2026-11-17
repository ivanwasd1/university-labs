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

    private static void printMatrix(StringBuilder text, String title, double[][] a) {
        text.append('\n').append(title).append('\n');
        for (double[] row : a) {
            for (double value : row)
                text.append(String.format(Locale.US, "% .12e  ", value));
            text.append('\n');
        }
    }

    public static void calculate(SystemData data, Path directory) throws IOException {
        double[][] a = data.a();
        double[] b = data.b();
        var gauss = GaussMethod.solve(a, b);
        double[][] inverse = gauss.inverse();
        double[][] r = LinearAlgebra.inverseResidual(inverse, a);
        double normA = LinearAlgebra.norm(a);
        double normInverse = LinearAlgebra.norm(inverse);
        double condition = normA * normInverse;
        double[] rowSums = LinearAlgebra.rowSums(r);
        double maxElement = LinearAlgebra.maxElement(r);
        double normR = LinearAlgebra.norm(r);
        if (!Double.isFinite(condition) || !Double.isFinite(normR))
            throw new ArithmeticException("Переполнение при проверке матрицы");
        var result = new LinkedHashMap<String, Object>();
        result.put("task", "СЛАУ, пункт а");
        result.put("method", "Метод Гаусса без выбора главного элемента");
        result.put("A", a);
        result.put("b", b);
        result.put("x", gauss.x());
        result.put("gauss_upper", gauss.upper());
        result.put("transformed_rhs", gauss.rhs());
        result.put("determinant", gauss.determinant());
        result.put("inverse", inverse);
        result.put("norm_A", normA);
        result.put("norm_inverse", normInverse);
        result.put("condition", condition);
        result.put("inverse_residual", r);
        result.put("inverse_residual_row_sums", rowSums);
        result.put("norm_inverse_residual", normR);
        result.put("inverse_residual_max_element", maxElement);
        StringBuilder text = new StringBuilder("СЛАУ. Пункт а)\n");
        text.append("Метод Гаусса без выбора главного элемента\n");
        text.append("x = ").append(Arrays.toString(gauss.x())).append('\n');
        text.append("det(A) = ").append(gauss.determinant()).append('\n');
        text.append("||A||_inf = ").append(normA).append('\n');
        text.append("||A^-1||_inf = ").append(normInverse).append('\n');
        text.append("nu_inf(A) = ").append(condition).append('\n');
        text.append("||R||_inf = ").append(normR).append('\n');
        text.append("max|Rij| = ").append(maxElement).append('\n');
        printMatrix(text, "Верхнетреугольная матрица U", gauss.upper());
        printMatrix(text, "Преобразованные правые части [c | F]", gauss.rhs());
        printMatrix(text, "Обратная матрица A^-1", inverse);
        printMatrix(text, "Матрица невязки R = E - A^-1*A", r);
        text.append("Суммы модулей строк R: ").append(Arrays.toString(rowSums));
        text.append('\n');
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("results.json"), json(result), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("results.txt"), text, StandardCharsets.UTF_8);
        System.out.print(text);
        System.out.println("Результаты сохранены в " + directory.toAbsolutePath());
    }

    public static void main(String[] args) {
        try {
            SystemData data = variant12();
            Path directory = Path.of("results");
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--test")) { SelfTest.run(); return; }
                if (args[i].equals("--help")) {
                    System.out.println("Без аргументов: СЛАУ-12, пункт а).");
                    System.out.println("Параметры: --input файл --out папка --test");
                    return;
                }
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Не задано значение " + args[i]);
                String key = args[i], value = args[++i];
                switch (key) {
                    case "--input" -> data = parse(Files.readString(Path.of(value), StandardCharsets.UTF_8));
                    case "--out" -> directory = Path.of(value);
                    default -> throw new IllegalArgumentException("Неизвестный параметр " + key);
                }
            }
            calculate(data, directory);
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}
