package lab6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;

public class Main {
    private static final int PROGRAMMER_ID = 3;
    private static final int PROGRAMMER_SALARY = 4000;
    private static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");
    private static final Set<Locale> SUPPORTED_LOCALES = Set.of(RUSSIAN, Locale.forLanguageTag("be-BY"), Locale.UK);

    public static void main(String[] args) {
        String memoryMessage = "Недостаточно памяти для выполнения программы.";
        try {
            Locale requestedLocale = args.length >= 2 ? new Locale(args[0], args[1]) : RUSSIAN;
            boolean supported = SUPPORTED_LOCALES.contains(requestedLocale);
            Locale locale = supported ? requestedLocale : RUSSIAN;
            ResourceBundle messages = ResourceBundle.getBundle("lab6.messages", locale);
            memoryMessage = messages.getString("error.memory");

            if (!supported || (args.length != 0 && args.length != 2)) {
                System.err.println(messages.getString("error.arguments"));
                return;
            }
            runDemo(messages);
        } catch (OutOfMemoryError error) {
            System.err.println(memoryMessage);
        }
    }

    private static void runDemo(ResourceBundle messages) {
        Path file = Path.of("employees_stage2.dat");
        EmployeeFileConnector connector = new EmployeeFileConnector(file);
        try {
            if (Files.notExists(file)) {
                Employee[] employees = {
                    new Manager(1, "Анна Коваль", 4500, 6),
                    new Analyst(2, "Иван Мороз", 3400, "BI"),
                    new Programmer(PROGRAMMER_ID, "Максим Сокол", PROGRAMMER_SALARY, "Java"),
                    new Tester(4, "Юлия Левченко", 2900, "API"),
                    new Designer(5, "Ольга Гринь", 3200, "Figma")
                };
                connector.save(employees);
                System.out.println(messages.getString("demo.saved"));
            }

            Employee[] restored = connector.read();
            System.out.println(messages.getString("demo.read"));
            for (Employee employee : restored) {
                System.out.println(employee.describe(messages));
            }

            Employee employee = connector.findById(PROGRAMMER_ID);
            System.out.println(messages.getString("demo.found"));
            System.out.println(employee.describe(messages));

            int newSalary = employee.getSalary() == PROGRAMMER_SALARY ? 4200 : PROGRAMMER_SALARY;
            employee.setSalary(newSalary);
            connector.update(employee);
            System.out.println(messages.getString("demo.updated"));
            System.out.println(connector.findById(PROGRAMMER_ID).describe(messages));

            System.out.println(messages.getString("demo.errors"));
            try {
                connector.findById(99);
            } catch (NoSuchElementException exception) {
                System.out.println(describeMissing(exception, messages));
            }
            try {
                employee.setSalary(0);
            } catch (IllegalArgumentException exception) {
                System.out.println(messages.getString(exception.getMessage()));
            }
        } catch (IOException exception) {
            System.err.println(messages.getString("error.file"));
        } catch (ClassNotFoundException exception) {
            System.err.println(messages.getString("error.class"));
        } catch (NoSuchElementException exception) {
            System.err.println(describeMissing(exception, messages));
        } catch (IllegalArgumentException exception) {
            System.err.println(messages.getString(exception.getMessage()));
        }
    }

    private static String describeMissing(NoSuchElementException exception, ResourceBundle messages) {
        return messages.getString("error.notFound").formatted(exception.getMessage());
    }
}
