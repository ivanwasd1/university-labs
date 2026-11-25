package lab6;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;

public final class EmployeeFileConnector {
    private final Path file;

    public EmployeeFileConnector(Path file) {
        this.file = file;
    }

    public void save(Employee[] employees) throws IOException {
        Path temporaryFile = Files.createTempFile(file.resolveSibling("."), "employees-", ".tmp");
        try {
            try (OutputStream fileOutput = Files.newOutputStream(temporaryFile);
                 ObjectOutputStream output = new ObjectOutputStream(fileOutput)) {
                output.writeObject(employees);
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public Employee[] read() throws IOException, ClassNotFoundException {
        try (InputStream fileInput = Files.newInputStream(file);
             ObjectInputStream input = new ObjectInputStream(fileInput)) {
            if (input.readObject() instanceof Employee[] employees) {
                return employees;
            }
            throw new InvalidObjectException("Файл не содержит массив работников.");
        }
    }

    public Employee findById(int id) throws IOException, ClassNotFoundException {
        Employee[] employees = read();
        return employees[findIndex(employees, id)];
    }

    public void update(Employee employee) throws IOException, ClassNotFoundException {
        Employee[] employees = read();
        employees[findIndex(employees, employee.getId())] = employee;
        save(employees);
    }

    private int findIndex(Employee[] employees, int id) {
        for (int index = 0; index < employees.length; index++) {
            if (employees[index].getId() == id) {
                return index;
            }
        }
        throw new NoSuchElementException("Работник с ID " + id + " не найден.");
    }
}
