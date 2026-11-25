package lab6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

public class Main {
    private static final int PROGRAMMER_ID = 3;
    private static final int PROGRAMMER_SALARY = 4000;

    public static void main(String[] args) {
        try {
            runDemo();
        } catch (OutOfMemoryError error) {
            System.err.println("Недостаточно памяти для выполнения программы.");
        } catch (IOException exception) {
            System.err.println("Не удалось прочитать или сохранить данные работников.");
        } catch (ClassNotFoundException exception) {
            System.err.println("Не удалось восстановить класс работника из файла.");
        } catch (NoSuchElementException | IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private static void runDemo() throws IOException, ClassNotFoundException {
        Path file = Path.of("employees_stage1.dat");
        EmployeeFileConnector connector = new EmployeeFileConnector(file);

        if (Files.notExists(file)) {
            Employee[] employees = {
                new Manager(1, "Анна Коваль", 4500, 6),
                new Analyst(2, "Иван Мороз", 3400, "BI"),
                new Programmer(PROGRAMMER_ID, "Максим Сокол", PROGRAMMER_SALARY, "Java"),
                new Tester(4, "Юлия Левченко", 2900, "API"),
                new Designer(5, "Ольга Гринь", 3200, "Figma")
            };
            connector.save(employees);
            System.out.println("Созданы и сохранены работники пяти профессий.");
        }

        Employee[] restored = connector.read();
        System.out.println("Работники, прочитанные из файла");
        for (Employee employee : restored) {
            System.out.println(employee);
        }

        Employee employee = connector.findById(PROGRAMMER_ID);
        System.out.println("Результат поиска по ID");
        System.out.println(employee);

        int newSalary = employee.getSalary() == PROGRAMMER_SALARY ? 4200 : PROGRAMMER_SALARY;
        employee.setSalary(newSalary);
        connector.update(employee);
        System.out.println("Работник после изменения зарплаты и повторного чтения из файла");
        System.out.println(connector.findById(PROGRAMMER_ID));

        System.out.println("Проверка обработки ошибок");
        try {
            connector.findById(99);
        } catch (NoSuchElementException exception) {
            System.out.println(exception.getMessage());
        }
        try {
            employee.setSalary(0);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }
}
