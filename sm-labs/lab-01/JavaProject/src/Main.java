import java.util.Arrays;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        double[][] a = {
                {0.5757, -0.0758, 0.0152, 0.0303, 0.1061},
                {0.0788, 0.9014, 0.0000, -0.0606, 0.0606},
                {0.0455, 0.0000, 0.7242, -0.2121, 0.1212},
                {-0.0909, 0.1909, 0.0000, 0.7121, -0.0303},
                {0.3788, 0.0000, 0.1364, 0.0152, 0.8484}
        };
        double[] b = {3.5148, 3.8542, -4.9056, 2.3240, 0.1818};

        GaussMethod.Result result = GaussMethod.solve(a, b);
        double[][] inverse = result.inverse();
        double[][] r = LinearAlgebra.inverseResidual(inverse, a);
        double normA = LinearAlgebra.norm(a);
        double normInverse = LinearAlgebra.norm(inverse);

        System.out.println("СЛАУ-12. Пункт а). Метод Гаусса без перестановок строк");
        printMatrix("Верхнетреугольная матрица U", result.upper(), "%14.9f");
        printMatrix("Преобразованные правые части [c | F]", result.rhs(), "%14.9f");

        System.out.println("\nx = " + Arrays.toString(result.x()));
        System.out.println("det A = " + result.determinant());
        printMatrix("Вычисленная обратная матрица Y", inverse, "%15.10f");
        System.out.println("||A||∞ = " + normA);
        System.out.println("||Y||∞ = " + normInverse);
        System.out.println("ν∞(A) = " + normA * normInverse);

        printMatrix("Матрица невязки R = E − YA", r, "%17.10e");
        System.out.println("||R||∞ = " + LinearAlgebra.norm(r));
        System.out.println("max |Rij| = " + LinearAlgebra.maxElement(r));
    }

    private static void printMatrix(String title, double[][] matrix, String format) {
        System.out.println("\n" + title);
        for (double[] row : matrix) {
            for (double value : row) {
                System.out.printf(Locale.ROOT, format + "  ", value);
            }
            System.out.println();
        }
    }
}
