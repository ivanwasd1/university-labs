package lab6;

import java.util.ResourceBundle;

public final class Programmer extends Employee {
    private static final long serialVersionUID = 1L;

    private final String programmingLanguage;

    public Programmer(int id, String name, int salary, String programmingLanguage) {
        super(id, name, salary);
        this.programmingLanguage = requireText(programmingLanguage, "error.programmingLanguage");
    }

    @Override
    protected String describeProfession(ResourceBundle messages) {
        return messages.getString("role.programmer").formatted(programmingLanguage);
    }
}
