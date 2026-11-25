package lab6;

public final class Analyst extends Employee {
    private static final long serialVersionUID = 1L;

    private final String analysisArea;

    public Analyst(int id, String name, int salary, String analysisArea) {
        super(id, name, salary);
        this.analysisArea = requireText(analysisArea, "Укажите область анализа.");
    }

    @Override
    protected String describeProfession() {
        return "Аналитик (область анализа %s)".formatted(analysisArea);
    }
}
