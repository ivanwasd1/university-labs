package lab6;

import java.util.ResourceBundle;

public final class Manager extends Employee {
    private static final long serialVersionUID = 1L;

    private final int teamSize;

    public Manager(int id, String name, int salary, int teamSize) {
        super(id, name, salary);
        if (teamSize <= 0) {
            throw new IllegalArgumentException("error.teamSize");
        }
        this.teamSize = teamSize;
    }

    @Override
    protected String describeProfession(ResourceBundle messages) {
        return messages.getString("role.manager").formatted(teamSize);
    }
}
