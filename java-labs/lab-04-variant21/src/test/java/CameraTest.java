import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {
    @Test
    void manualCameraConstructorsAndGetters() {
        ManualFocusCamera camera = new ManualFocusCamera("Zenit", 24, 500, 0.5);
        System.out.println("Ручная камера: " + camera);
        assertEquals("Zenit", camera.getModel());
        assertEquals(24, camera.getResolution());
        assertEquals(500, camera.getPrice());
        assertEquals(0.5, camera.getMinFocusDistance());
        assertEquals("Zenit;24.0;500.0;0.5", camera.toString());

        ManualFocusCamera restored = new ManualFocusCamera(camera.toString());
        System.out.println("Восстановленная камера: " + restored);
        assertEquals(camera.getModel(), restored.getModel());
        assertEquals(camera.getResolution(), restored.getResolution());
        assertEquals(camera.getPrice(), restored.getPrice());
        assertEquals(camera.getMinFocusDistance(), restored.getMinFocusDistance());
        assertEquals(camera.toString(), restored.toString());
    }

    @Test
    void autoCameraConstructorsAndGetters() {
        AutoFocusCamera camera = new AutoFocusCamera("Nikon", 20, 900, 51);
        System.out.println("Автофокусная камера: " + camera);
        assertEquals("Nikon", camera.getModel());
        assertEquals(20, camera.getResolution());
        assertEquals(900, camera.getPrice());
        assertEquals(51, camera.getFocusPoints());
        assertEquals("Nikon;20.0;900.0;51", camera.toString());

        AutoFocusCamera restored = new AutoFocusCamera(camera.toString());
        System.out.println("Восстановленная камера: " + restored);
        assertEquals(camera.getModel(), restored.getModel());
        assertEquals(camera.getResolution(), restored.getResolution());
        assertEquals(camera.getPrice(), restored.getPrice());
        assertEquals(camera.getFocusPoints(), restored.getFocusPoints());
        assertEquals(camera.toString(), restored.toString());
        assertEquals(0, new AutoFocusCamera("Canon", 12, 0, 1).getPrice());
    }

    @Test
    void manualCameraSorting() {
        ManualFocusCamera a = new ManualFocusCamera("A", 40, 500, 1.0);
        ManualFocusCamera b = new ManualFocusCamera("B", 20, 900, 1.5);
        ManualFocusCamera c = new ManualFocusCamera("C", 30, 700, 0.5);
        ManualFocusCamera[] cameras = {c, b, a};
        ManualFocusCamera[][] expected = {{a, b, c}, {b, c, a}, {a, c, b}, {c, a, b}};
        System.out.println("Исходные ручные камеры: " + Arrays.toString(cameras));
        for (int field = 0; field < 4; field++) {
            Camera.setComparisonField(field);
            ManualFocusCamera[] comparable = cameras.clone();
            Arrays.sort(comparable);
            ManualFocusCamera[] comparator = cameras.clone();
            Arrays.sort(comparator, a);
            System.out.println("Поле " + field + ", Comparable: " + Arrays.toString(comparable));
            System.out.println("Поле " + field + ", Comparator: " + Arrays.toString(comparator));
            assertArrayEquals(expected[field], comparable);
            assertArrayEquals(expected[field], comparator);
            assertTrue(expected[field][0].compareTo(expected[field][1]) < 0);
            assertTrue(a.compare(expected[field][1], expected[field][0]) > 0);
            assertEquals(0, a.compareTo(a));
            assertEquals(0, a.compare(a, a));
        }
        Camera.setComparisonField(0);
    }

    @Test
    void autoCameraSorting() {
        AutoFocusCamera a = new AutoFocusCamera("A", 40, 500, 30);
        AutoFocusCamera b = new AutoFocusCamera("B", 20, 900, 20);
        AutoFocusCamera c = new AutoFocusCamera("C", 30, 700, 10);
        AutoFocusCamera[] cameras = {c, b, a};
        AutoFocusCamera[][] expected = {{a, b, c}, {b, c, a}, {a, c, b}, {c, b, a}};
        System.out.println("Исходные автофокусные камеры: " + Arrays.toString(cameras));
        for (int field = 0; field < 4; field++) {
            Camera.setComparisonField(field);
            AutoFocusCamera[] comparable = cameras.clone();
            Arrays.sort(comparable);
            AutoFocusCamera[] comparator = cameras.clone();
            Arrays.sort(comparator, a);
            System.out.println("Поле " + field + ", Comparable: " + Arrays.toString(comparable));
            System.out.println("Поле " + field + ", Comparator: " + Arrays.toString(comparator));
            assertArrayEquals(expected[field], comparable);
            assertArrayEquals(expected[field], comparator);
            assertTrue(expected[field][0].compareTo(expected[field][1]) < 0);
            assertTrue(a.compare(expected[field][1], expected[field][0]) > 0);
            assertEquals(0, a.compareTo(a));
            assertEquals(0, a.compare(a, a));
        }
        Camera.setComparisonField(0);
    }

    @Test
    void allFieldsCanBeIterated() {
        ManualFocusCamera manual = new ManualFocusCamera("Zenit", 24, 500, 0.5);
        AutoFocusCamera auto = new AutoFocusCamera("Nikon", 20, 900, 51);
        List<Object> manualFields = new ArrayList<>();
        for (Object field : manual) {
            manualFields.add(field);
        }
        List<Object> autoFields = new ArrayList<>();
        for (Object field : auto) {
            autoFields.add(field);
        }
        System.out.println(manual + " -> " + manualFields);
        System.out.println(auto + " -> " + autoFields);
        assertEquals(Arrays.asList("Zenit", 24.0, 500.0, 0.5), manualFields);
        assertEquals(Arrays.asList("Nikon", 20.0, 900.0, 51), autoFields);
        Iterator<Object> manualIterator = manual.iterator();
        Iterator<Object> autoIterator = auto.iterator();
        for (int i = 0; i < 4; i++) {
            assertTrue(manualIterator.hasNext());
            assertTrue(autoIterator.hasNext());
            manualIterator.next();
            autoIterator.next();
        }
        assertFalse(manualIterator.hasNext());
        assertFalse(autoIterator.hasNext());
        assertThrows(NoSuchElementException.class, manualIterator::next);
        assertThrows(NoSuchElementException.class, autoIterator::next);
        System.out.println("После четвертого поля итераторы завершены.");
    }

    @Test
    void invalidConstructorArguments() {
        for (String model : new String[]{null, "", "   ", "A;B"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ManualFocusCamera(model, 24, 500, 0.5));
            assertThrows(IllegalArgumentException.class,
                    () -> new AutoFocusCamera(model, 24, 500, 51));
            System.out.println("Некорректная модель: " + model + " -> исключение");
        }
        for (double value : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ManualFocusCamera("Zenit", value, 500, 0.5));
            assertThrows(IllegalArgumentException.class,
                    () -> new AutoFocusCamera("Nikon", value, 500, 51));
            assertThrows(IllegalArgumentException.class,
                    () -> new ManualFocusCamera("Zenit", 24, 500, value));
            System.out.println("Разрешение или расстояние: " + value + " -> исключение");
        }
        for (double price : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ManualFocusCamera("Zenit", 24, price, 0.5));
            assertThrows(IllegalArgumentException.class,
                    () -> new AutoFocusCamera("Nikon", 24, price, 51));
            System.out.println("Цена: " + price + " -> исключение");
        }
        for (int points : new int[]{0, -1}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new AutoFocusCamera("Nikon", 24, 500, points));
            System.out.println("Точки фокусировки: " + points + " -> исключение");
        }
    }

    @Test
    void invalidTextAndComparisonField() {
        for (String data : new String[]{null, "", "A;24;500", "A;24;500;1;2",
                "A;abc;500;1", "A;24;500;", ";24;500;1", "A;24;500;0"}) {
            assertThrows(IllegalArgumentException.class, () -> new ManualFocusCamera(data));
            assertThrows(IllegalArgumentException.class, () -> new AutoFocusCamera(data));
            System.out.println("Строка: " + data + " -> исключение");
        }
        for (int field : new int[]{-1, 4}) {
            assertThrows(IllegalArgumentException.class, () -> Camera.setComparisonField(field));
            System.out.println("Поле сравнения: " + field + " -> исключение");
        }
    }
}
