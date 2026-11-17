import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ManualFocusCamera[] manualCameras = {
                new ManualFocusCamera("Zenit", 24, 35000, 0.5),
                new ManualFocusCamera("Leica", 40, 120000, 0.7),
                new ManualFocusCamera("Nikon", 20, 50000, 0.3)
        };
        AutoFocusCamera[] autoCameras = {
                new AutoFocusCamera("Canon", 32, 90000, 45),
                new AutoFocusCamera("Sony", 24, 110000, 425),
                new AutoFocusCamera("Nikon", 20, 70000, 51)
        };

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Введите поле сортировки:");
            System.out.println("1 - модель, 2 - разрешение, 3 - цена");
            System.out.println("4 - расстояние фокусировки / количество точек автофокуса");
            if (!scanner.hasNextInt()) {
                System.out.println("Нужно ввести число от 1 до 4");
                return;
            }
            int field = scanner.nextInt();
            if (field < 1 || field > 4) {
                System.out.println("Нужно ввести число от 1 до 4");
                return;
            }
            Camera.setComparisonField(field - 1);
        }

        Arrays.sort(manualCameras);
        Arrays.sort(autoCameras, autoCameras[0]);

        System.out.println("Камеры с ручной фокусировкой:");
        for (ManualFocusCamera camera : manualCameras) {
            System.out.println(camera);
        }
        System.out.println("Камеры с автофокусом:");
        for (AutoFocusCamera camera : autoCameras) {
            System.out.println(camera);
        }

        ManualFocusCamera manualCopy = new ManualFocusCamera(manualCameras[0].toString());
        AutoFocusCamera autoCopy = new AutoFocusCamera(autoCameras[0].toString());
        System.out.println("Восстановленные камеры:");
        System.out.println(manualCopy);
        System.out.println(autoCopy);
        System.out.println("Поля камеры с ручной фокусировкой:");
        for (Object field : manualCopy) {
            System.out.println(field);
        }
        System.out.println("Поля камеры с автофокусом:");
        for (Object field : autoCopy) {
            System.out.println(field);
        }
    }
}
