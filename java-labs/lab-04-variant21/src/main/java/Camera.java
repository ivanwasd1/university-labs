public abstract class Camera {
    private final String model;
    private final double resolution;
    private final double price;
    protected static int comparisonField;

    protected Camera(String model, double resolution, double price) {
        if (model == null || model.isBlank() || model.contains(";")) {
            throw new IllegalArgumentException("Некорректная модель");
        }
        if (!Double.isFinite(resolution) || resolution <= 0) {
            throw new IllegalArgumentException("Разрешение должно быть положительным");
        }
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        this.model = model;
        this.resolution = resolution;
        this.price = price;
    }

    public String getModel() {
        return model;
    }

    public double getResolution() {
        return resolution;
    }

    public double getPrice() {
        return price;
    }

    public static void setComparisonField(int field) {
        if (field < 0 || field > 3) {
            throw new IllegalArgumentException("Поле должно быть от 0 до 3");
        }
        comparisonField = field;
    }

    protected int compareCommonFields(Camera other) {
        return switch (comparisonField) {
            case 0 -> model.compareTo(other.model);
            case 1 -> Double.compare(resolution, other.resolution);
            case 2 -> Double.compare(price, other.price);
            default -> throw new IllegalArgumentException("Некорректное поле сравнения");
        };
    }

    protected static String[] parse(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Строка не задана");
        }
        String[] fields = data.split(";", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("В строке должно быть четыре поля");
        }
        return fields;
    }
}
