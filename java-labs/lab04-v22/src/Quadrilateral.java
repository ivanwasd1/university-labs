public class Quadrilateral {
    private static final double EPS = 0.00000001;
    private final Point[] points;

    // Вершины передаются по порядку обхода границы.
    public Quadrilateral(Point a, Point b, Point c, Point d) {
        points = new Point[] {a, b, c, d};

        for (int i = 0; i < 4; i++) {
            if (points[i] == null) {
                throw new IllegalArgumentException("Не все вершины заданы");
            }
            for (int j = 0; j < i; j++) {
                if (points[i].distanceTo(points[j]) < EPS) {
                    throw new IllegalArgumentException("Вершины совпадают");
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            if (Math.abs(cross(points[i], points[(i + 1) % 4], points[(i + 2) % 4])) < EPS) {
                throw new IllegalArgumentException("Три вершины лежат на одной прямой");
            }
        }
        if (intersects(a, b, c, d) || intersects(b, c, d, a)) {
            throw new IllegalArgumentException("Стороны пересекаются");
        }
        if (!Double.isFinite(getArea()) || !Double.isFinite(getPerimeter()) || getArea() < EPS) {
            throw new IllegalArgumentException("Невозможно вычислить размеры фигуры");
        }
        assert getArea() > 0 : "Площадь должна быть положительной";
    }

    // Векторное произведение AB и AC.
    private double cross(Point a, Point b, Point c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY())
             - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private boolean intersects(Point a, Point b, Point c, Point d) {
        return cross(a, b, c) * cross(a, b, d) < 0
            && cross(c, d, a) * cross(c, d, b) < 0;
    }

    public double getArea() {
        // Сумма ориентированных площадей треугольников ABC и ACD.
        return Math.abs(cross(points[0], points[1], points[2])
                      + cross(points[0], points[2], points[3])) / 2;
    }

    public double getPerimeter() {
        double perimeter = 0;
        for (int i = 0; i < 4; i++) {
            perimeter += points[i].distanceTo(points[(i + 1) % 4]);
        }
        return perimeter;
    }

    public String getType() {
        boolean equalSides = true;
        boolean rightAngles = true;
        double firstSide = points[0].distanceTo(points[1]);

        for (int i = 0; i < 4; i++) {
            Point a = points[i];
            Point b = points[(i + 1) % 4];
            Point c = points[(i + 2) % 4];
            if (Math.abs(a.distanceTo(b) - firstSide) > EPS) {
                equalSides = false;
            }
            // Скалярное произведение BA и BC равно нулю при прямом угле.
            double dot = (a.getX() - b.getX()) * (c.getX() - b.getX())
                       + (a.getY() - b.getY()) * (c.getY() - b.getY());
            if (Math.abs(dot) > EPS) {
                rightAngles = false;
            }
        }
        if (equalSides && rightAngles) {
            return "Квадрат";
        }
        if (rightAngles) {
            return "Прямоугольник";
        }
        if (equalSides) {
            return "Ромб";
        }
        return "Произвольный";
    }

    public String toString() {
        return points[0] + " " + points[1] + " " + points[2] + " " + points[3];
    }
}
