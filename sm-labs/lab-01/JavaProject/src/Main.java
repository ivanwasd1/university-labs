import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    public record SystemData(double[][] a, double[] b) {}

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--test")) {
            SelfTest.run();
            return;
        }

        try {
            SystemData data = variant12();
            Path output = null;
            for (int i = 0; i < args.length; i += 2) {
                String option = args[i];
                if (i + 1 == args.length) {
                    throw new IllegalArgumentException("Не задано значение " + option);
                }
                String value = args[i + 1];
                switch (option) {
                    case "--input" -> data = parse(Files.readString(Path.of(value), StandardCharsets.UTF_8));
                    case "--out" -> output = Path.of(value);
                    default -> throw new IllegalArgumentException("Неизвестный параметр " + option);
                }
            }

            String result = calculate(data);
            System.out.print(result);
            if (output != null) {
                Files.createDirectories(output);
                Files.writeString(output.resolve("results.txt"), result, StandardCharsets.UTF_8);
            }
        } catch (IOException | IllegalArgumentException | ArithmeticException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    public static SystemData variant12() {
        double[][] a = {
                {0.5757, -0.0758, 0.0152, 0.0303, 0.1061},
                {0.0788, 0.9014, 0.0000, -0.0606, 0.0606},
                {0.0455, 0.0000, 0.7242, -0.2121, 0.1212},
                {-0.0909, 0.1909, 0.0000, 0.7121, -0.0303},
                {0.3788, 0.0000, 0.1364, 0.0152, 0.8484}
        };
        double[] b = {3.5148, 3.8542, -4.9056, 2.3240, 0.1818};
        return new SystemData(a, b);
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

    private static String calculate(SystemData data) {
        double[][] a = data.a();
        GaussMethod.Result result = GaussMethod.solve(a, data.b());
        double[][] inverse = result.inverse();
        double[][] r = LinearAlgebra.inverseResidual(inverse, a);
        double normA = LinearAlgebra.norm(a);
        double normInverse = LinearAlgebra.norm(inverse);
        double condition = normA * normInverse;
        double normR = LinearAlgebra.norm(r);
        if (!Double.isFinite(condition) || !Double.isFinite(normR)) {
            throw new ArithmeticException("Переполнение при проверке матрицы");
        }

        StringBuilder text = new StringBuilder("СЛАУ. Пункт а). Метод Гаусса без перестановок строк\n");
        text.append("x = ").append(Arrays.toString(result.x())).append('\n');
        text.append("det A = ").append(result.determinant()).append('\n');
        text.append("||A||∞ = ").append(normA).append('\n');
        text.append("||Y||∞ = ").append(normInverse).append('\n');
        text.append("ν∞(A) = ").append(condition).append('\n');
        text.append("||R||∞ = ").append(normR).append('\n');
        text.append("max |Rij| = ").append(LinearAlgebra.maxElement(r)).append('\n');

        printMatrix(text, "Верхнетреугольная матрица U", result.upper(), "%14.9f  ");
        printMatrix(text, "Преобразованные правые части [c | F]", result.rhs(), "%14.9f  ");
        printMatrix(text, "Вычисленная обратная матрица Y", inverse, "%15.10f  ");
        printMatrix(text, "Матрица невязки R = E − YA", r, "%17.10e  ");
        text.append("Суммы модулей строк R: ").append(Arrays.toString(LinearAlgebra.rowSums(r))).append('\n');
        return text.toString();
    }

    private static void printMatrix(StringBuilder text, String title, double[][] matrix, String format) {
        text.append('\n').append(title).append('\n');
        for (double[] row : matrix) {
            for (double value : row) text.append(String.format(Locale.ROOT, format, value));
            text.append('\n');
        }
    }
}
