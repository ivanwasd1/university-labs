package lab6;

import java.util.ResourceBundle;

public final class Analyst extends Employee {
    private static final long serialVersionUID = 1L;

    private final String analysisArea;

    public Analyst(int id, String name, int salary, String analysisArea) {
        super(id, name, salary);
        this.analysisArea = requireText(analysisArea, "error.analysisArea");
    }

    @Override
    protected String describeProfession(ResourceBundle messages) {
        return messages.getString("role.analyst").formatted(analysisArea);
    }
}
