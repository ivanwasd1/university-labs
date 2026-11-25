package lab6;

import java.io.Serializable;

public abstract class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private int salary;

    protected Employee(int id, String name, int salary) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID работника должен быть больше нуля.");
        }
        this.id = id;
        this.name = requireText(name, "Имя работника не должно быть пустым.");
        setSalary(salary);
    }

    public int getId() {
        return id;
    }

    public int getSalary() {
        return salary;
    }

    public final void setSalary(int salary) {
        if (salary <= 0) {
            throw new IllegalArgumentException("Зарплата должна быть больше нуля.");
        }
        this.salary = salary;
    }

    protected static String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    protected abstract String describeProfession();

    @Override
    public String toString() {
        return describeProfession() + " | ID " + id + " | " + name + " | зарплата " + salary;
    }
}
