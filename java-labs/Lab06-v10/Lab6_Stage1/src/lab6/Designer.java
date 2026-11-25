package lab6;

public final class Designer extends Employee {
    private static final long serialVersionUID = 1L;

    private final String designTool;

    public Designer(int id, String name, int salary, String designTool) {
        super(id, name, salary);
        this.designTool = requireText(designTool, "Укажите инструмент дизайнера.");
    }

    @Override
    protected String describeProfession() {
        return "Дизайнер (инструмент %s)".formatted(designTool);
    }
}
