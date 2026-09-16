import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        run();
    }

    private static void run() {

        int matrix[][] = fillMatrixWithRandomInts();
        sortMatrixByColumnCharacteristic(matrix);
        printMatrix(matrix);
    }

    private static int[][] fillMatrixWithRandomInts() {

        System.out.print("Введите размерность матрицы: ");
        int n = new Scanner(System.in).nextInt();
        int matrix[][] = new int[n][];

        Random random = new Random();
        for (int i = 0; i < n; i++) {
            matrix[i] = random.ints(n, -n, n + 1).toArray();
        }
        return matrix;
    }

    private static void sortMatrixByColumnCharacteristic(int[][] matrix) {

        transpose(matrix);

        Arrays.sort(matrix, (ints1, ints2) ->
                Integer.compare(
                        Arrays.stream(ints2).map(Math::abs).sum(),
                        Arrays.stream(ints1).map(Math::abs).sum()
                )
        );

        transpose(matrix);
    }

    private static void transpose(int[][] matrix) {

        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix.length; j++) {
                int temp = matrix[j][i];
                matrix[j][i] = matrix[i][j];
                matrix[i][j] = temp;
            }
        }
    }
    private static void printMatrix(int[][] matrix) {

        System.out.println("Полученная матрица");
        for (int[] r : matrix) {
            for (int c : r) {
                System.out.printf("%5d", c);
            }
            System.out.println();
        }
    }
}