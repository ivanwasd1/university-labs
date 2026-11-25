import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Random random = new Random();
        Quadrilateral[] figures = new Quadrilateral[20];

        for (int i = 0; i < figures.length; i++) {
            figures[i] = createRandom(random);
            System.out.println("№ " + (i + 1) + ": " + figures[i].getType());
            System.out.println("Вершины: " + figures[i]);
            System.out.printf("Площадь: %.2f; периметр: %.2f%n%n",
                    figures[i].getArea(), figures[i].getPerimeter());
        }
        printStatistics(figures);
    }

    public static Quadrilateral createRandom(Random random) {
        int x = random.nextInt(21) - 10;
        int y = random.nextInt(21) - 10;
        int width = random.nextInt(8) + 1;
        int height = random.nextInt(8) + 1;
        int type = random.nextInt(4);

        // Сначала выбираем тип, чтобы в массив попадали и правильные фигуры.
        if (type == 0) {
            return new Quadrilateral(new Point(x, y), new Point(x + width, y),
                    new Point(x + width, y + width), new Point(x, y + width));
        }
        if (type == 1) {
            height = width + random.nextInt(5) + 1;
            return new Quadrilateral(new Point(x, y), new Point(x + width, y),
                    new Point(x + width, y + height), new Point(x, y + height));
        }
        if (type == 2) {
            height = width + random.nextInt(5) + 1;
            return new Quadrilateral(new Point(x - width, y), new Point(x, y + height),
                    new Point(x + width, y), new Point(x, y - height));
        }
        // Трапеция: основания заведомо имеют разную длину.
        return new Quadrilateral(new Point(x, y), new Point(x + width + 1, y),
                new Point(x + width, y + height), new Point(x, y + height));
    }

    public static void printStatistics(Quadrilateral[] figures) {
        String[] types = {"Квадрат", "Прямоугольник", "Ромб", "Произвольный"};

        System.out.println("РЕЗУЛЬТАТЫ ПО ГРУППАМ");
        for (String type : types) {
            int count = 0;
            int minArea = -1;
            int maxArea = -1;
            int minPerimeter = -1;
            int maxPerimeter = -1;

            for (int i = 0; i < figures.length; i++) {
                if (figures[i].getType().equals(type)) {
                    count++;
                    if (minArea == -1 || figures[i].getArea() < figures[minArea].getArea()) {
                        minArea = i;
                    }
                    if (maxArea == -1 || figures[i].getArea() > figures[maxArea].getArea()) {
                        maxArea = i;
                    }
                    if (minPerimeter == -1 || figures[i].getPerimeter() < figures[minPerimeter].getPerimeter()) {
                        minPerimeter = i;
                    }
                    if (maxPerimeter == -1 || figures[i].getPerimeter() > figures[maxPerimeter].getPerimeter()) {
                        maxPerimeter = i;
                    }
                }
            }
            System.out.println("\n" + type + ": " + count);
            if (count == 0) {
                System.out.println("Нет объектов");
            } else {
                System.out.printf("Минимум площади: № %d, S = %.2f%n", minArea + 1, figures[minArea].getArea());
                System.out.printf("Максимум площади: № %d, S = %.2f%n", maxArea + 1, figures[maxArea].getArea());
                System.out.printf("Минимум периметра: № %d, P = %.2f%n", minPerimeter + 1, figures[minPerimeter].getPerimeter());
                System.out.printf("Максимум периметра: № %d, P = %.2f%n", maxPerimeter + 1, figures[maxPerimeter].getPerimeter());
            }
        }
    }
}
