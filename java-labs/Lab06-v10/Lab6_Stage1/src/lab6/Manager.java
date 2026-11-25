package lab6;

public final class Manager extends Employee {
    private static final long serialVersionUID = 1L;

    private final int teamSize;

    public Manager(int id, String name, int salary, int teamSize) {
        super(id, name, salary);
        if (teamSize <= 0) {
            throw new IllegalArgumentException("В команде должен быть хотя бы один сотрудник.");
        }
        this.teamSize = teamSize;
    }

    @Override
    protected String describeProfession() {
        return "Менеджер (размер команды %s)".formatted(teamSize);
    }
}
