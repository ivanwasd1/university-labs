import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        double x = Double.parseDouble(args[0]);
        int k = Integer.parseInt(args[1]);

        double eps = Math.pow(10, -k - 2);
        double a = Math.IEEEremainder(x, 2 * Math.PI);

        double term = a;
        double sum = 0;
        int n = 1;

        while (Math.abs(term) >= eps && term != 0) {
            sum += term;
            term *= -a * a / ((2.0 * n) * (2.0 * n + 1));
            n++;
        }

        System.out.printf(Locale.US, "Taylor: %." + k + "f%n", sum);
        System.out.printf(Locale.US, "Math:   %." + k + "f%n", Math.sin(x));
    }
}