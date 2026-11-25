package lab6;

public final class Tester extends Employee {
    private static final long serialVersionUID = 1L;

    private final String testObject;

    public Tester(int id, String name, int salary, String testObject) {
        super(id, name, salary);
        this.testObject = requireText(testObject, "Укажите объект тестирования.");
    }

    @Override
    protected String describeProfession() {
        return "Тестировщик (объект тестирования %s)".formatted(testObject);
    }
}
