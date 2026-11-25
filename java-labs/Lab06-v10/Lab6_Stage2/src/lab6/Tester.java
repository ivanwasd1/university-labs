package lab6;

import java.util.ResourceBundle;

public final class Tester extends Employee {
    private static final long serialVersionUID = 1L;

    private final String testObject;

    public Tester(int id, String name, int salary, String testObject) {
        super(id, name, salary);
        this.testObject = requireText(testObject, "error.testObject");
    }

    @Override
    protected String describeProfession(ResourceBundle messages) {
        return messages.getString("role.tester").formatted(testObject);
    }
}
