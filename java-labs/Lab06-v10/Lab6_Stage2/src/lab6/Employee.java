package lab6;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public abstract class Employee implements Serializable {
    private static final long serialVersionUID = 2L;

    private final int id;
    private final String name;
    private int salary;
    private final Date createdAt;

    protected Employee(int id, String name, int salary) {
        if (id <= 0) {
            throw new IllegalArgumentException("error.id");
        }
        this.id = id;
        this.name = requireText(name, "error.name");
        setSalary(salary);
        createdAt = new Date();
    }

    public int getId() {
        return id;
    }

    public int getSalary() {
        return salary;
    }

    public final void setSalary(int salary) {
        if (salary <= 0) {
            throw new IllegalArgumentException("error.salary");
        }
        this.salary = salary;
    }

    protected static String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    protected abstract String describeProfession(ResourceBundle messages);

    public String describe(ResourceBundle messages) {
        Locale locale = messages.getLocale();
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        return messages.getString("employee.line").formatted(
                describeProfession(messages), id, name, numberFormat.format(salary), dateFormat.format(createdAt));
    }
}
