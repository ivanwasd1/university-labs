import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import static org.junit.Assert.*;

public class QuadrilateralTest {
    private static final double EPS = 0.000001;

    @Test
    public void pointTest() {
        Point a = new Point(1, 2);
        Point b = new Point(4, 6);
        System.out.println("Точки " + a + " и " + b + ": расстояние = " + a.distanceTo(b));
        assertEquals(1, a.getX(), EPS);
        assertEquals(2, a.getY(), EPS);
        assertEquals(5, a.distanceTo(b), EPS);
        assertEquals(0, a.distanceTo(a), EPS);
        assertEquals("(1.0; 2.0)", a.toString());
    }

    @Test
    public void squareTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(2, 0),
                new Point(2, 2), new Point(0, 2));
        checkFigure(q, "Квадрат", 4, 8);
        assertEquals("(0.0; 0.0) (2.0; 0.0) (2.0; 2.0) (0.0; 2.0)", q.toString());
    }

    @Test
    public void rectangleTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(4, 0),
                new Point(4, 3), new Point(0, 3));
        checkFigure(q, "Прямоугольник", 12, 14);
    }

    @Test
    public void rhombusTest() {
        Quadrilateral q = new Quadrilateral(new Point(-2, 0), new Point(0, 1),
                new Point(2, 0), new Point(0, -1));
        checkFigure(q, "Ромб", 4, 4 * Math.sqrt(5));
    }

    @Test
    public void arbitraryTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(4, 0),
                new Point(3, 2), new Point(0, 2));
        checkFigure(q, "Произвольный", 7, 9 + Math.sqrt(5));
    }

    @Test
    public void concaveTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(4, 0),
                new Point(1, 1), new Point(0, 4));
        checkFigure(q, "Произвольный", 4, 8 + 2 * Math.sqrt(10));
    }

    @Test
    public void clockwiseTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(0, 2),
                new Point(2, 2), new Point(2, 0));
        checkFigure(q, "Квадрат", 4, 8);
    }

    @Test
    public void rotatedSquareTest() {
        Quadrilateral q = new Quadrilateral(new Point(0, 0), new Point(1, 1),
                new Point(0, 2), new Point(-1, 1));
        checkFigure(q, "Квадрат", 2, 4 * Math.sqrt(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCoordinateTest() {
        System.out.println("Точка (NaN; 0): ожидается IllegalArgumentException");
        new Point(Double.NaN, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPointTest() {
        System.out.println("Расстояние до null: ожидается IllegalArgumentException");
        new Point(0, 0).distanceTo(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullVertexTest() {
        System.out.println("Вершина null: ожидается IllegalArgumentException");
        new Quadrilateral(new Point(0, 0), null, new Point(2, 2), new Point(0, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void repeatedVerticesTest() {
        System.out.println("Совпадающие вершины (0; 0): ожидается IllegalArgumentException");
        new Quadrilateral(new Point(0, 0), new Point(0, 0), new Point(2, 2), new Point(0, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void collinearTest() {
        System.out.println("Вершины (0; 0), (1; 0), (2; 0) на прямой: ожидается IllegalArgumentException");
        new Quadrilateral(new Point(0, 0), new Point(1, 0), new Point(2, 0), new Point(0, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void intersectionTest() {
        System.out.println("Самопересечение (0; 0), (2; 2), (0; 2), (2; 0): ожидается IllegalArgumentException");
        new Quadrilateral(new Point(0, 0), new Point(2, 2), new Point(0, 2), new Point(2, 0));
    }

    private void checkFigure(Quadrilateral q, String type, double area, double perimeter) {
        System.out.println("\nВершины: " + q);
        System.out.println("Ожидалось: " + type + ", S = " + area + ", P = " + perimeter);
        System.out.println("Получено:  " + q.getType() + ", S = " + q.getArea() + ", P = " + q.getPerimeter());
        assertEquals(type, q.getType());
        assertEquals(area, q.getArea(), EPS);
        assertEquals(perimeter, q.getPerimeter(), EPS);
        System.out.println("Проверка пройдена");
    }

    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(QuadrilateralTest.class);
        for (Failure failure : result.getFailures()) {
            System.out.println(failure);
        }
        System.out.println("\nТестов: " + result.getRunCount() + ", ошибок: " + result.getFailureCount());
        if (!result.wasSuccessful()) {
            System.exit(1);
        }
    }
}
