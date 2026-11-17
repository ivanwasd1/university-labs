import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ManualFocusCamera extends Camera implements Comparable<ManualFocusCamera>,
        Comparator<ManualFocusCamera>, Iterable<Object> {
    private final double minFocusDistance;

    public ManualFocusCamera(String model, double resolution, double price, double minFocusDistance) {
        super(model, resolution, price);
        if (!Double.isFinite(minFocusDistance) || minFocusDistance <= 0) {
            throw new IllegalArgumentException("Расстояние фокусировки должно быть положительным");
        }
        this.minFocusDistance = minFocusDistance;
    }

    public ManualFocusCamera(String data) {
        this(parse(data));
    }

    private ManualFocusCamera(String[] fields) {
        this(fields[0], Double.parseDouble(fields[1]), Double.parseDouble(fields[2]),
                Double.parseDouble(fields[3]));
    }

    public double getMinFocusDistance() {
        return minFocusDistance;
    }

    @Override
    public int compareTo(ManualFocusCamera other) {
        return compare(this, other);
    }

    @Override
    public int compare(ManualFocusCamera first, ManualFocusCamera second) {
        if (comparisonField == 3) {
            return Double.compare(first.minFocusDistance, second.minFocusDistance);
        }
        return first.compareCommonFields(second);
    }

    @Override
    public Iterator<Object> iterator() {
        return List.<Object>of(getModel(), getResolution(), getPrice(), minFocusDistance).iterator();
    }

    @Override
    public String toString() {
        return getModel() + ";" + getResolution() + ";" + getPrice() + ";" + minFocusDistance;
    }
}
