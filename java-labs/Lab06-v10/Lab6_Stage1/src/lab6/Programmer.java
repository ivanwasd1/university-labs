package lab6;

public final class Programmer extends Employee {
    private static final long serialVersionUID = 1L;

    private final String programmingLanguage;

    public Programmer(int id, String name, int salary, String programmingLanguage) {
        super(id, name, salary);
        this.programmingLanguage = requireText(programmingLanguage, "Укажите язык программирования.");
    }

    @Override
    protected String describeProfession() {
        return "Программист (язык программирования %s)".formatted(programmingLanguage);
    }
}
