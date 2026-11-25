package lab6;

import java.util.ResourceBundle;

public final class Designer extends Employee {
    private static final long serialVersionUID = 1L;

    private final String designTool;

    public Designer(int id, String name, int salary, String designTool) {
        super(id, name, salary);
        this.designTool = requireText(designTool, "error.designTool");
    }

    @Override
    protected String describeProfession(ResourceBundle messages) {
        return messages.getString("role.designer").formatted(designTool);
    }
}
