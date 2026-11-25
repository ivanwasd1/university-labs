import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AutoFocusCamera extends Camera implements Comparable<AutoFocusCamera>,
        Comparator<AutoFocusCamera>, Iterable<Object> {
    private final int focusPoints;

    public AutoFocusCamera(String model, double resolution, double price, int focusPoints) {
        super(model, resolution, price);
        if (focusPoints <= 0) {
            throw new IllegalArgumentException("Количество точек автофокуса должно быть положительным");
        }
        this.focusPoints = focusPoints;
    }

    public AutoFocusCamera(String data) {
        this(parse(data));
    }

    private AutoFocusCamera(String[] fields) {
        this(fields[0], Double.parseDouble(fields[1]), Double.parseDouble(fields[2]),
                Integer.parseInt(fields[3]));
    }

    public int getFocusPoints() {
        return focusPoints;
    }

    @Override
    public int compareTo(AutoFocusCamera other) {
        return compare(this, other);
    }

    @Override
    public int compare(AutoFocusCamera first, AutoFocusCamera second) {
        if (comparisonField == 3) {
            return Integer.compare(first.focusPoints, second.focusPoints);
        }
        return first.compareCommonFields(second);
    }

    @Override
    public Iterator<Object> iterator() {
        return List.<Object>of(getModel(), getResolution(), getPrice(), focusPoints).iterator();
    }

    @Override
    public String toString() {
        return getModel() + ";" + getResolution() + ";" + getPrice() + ";" + focusPoints;
    }
}
