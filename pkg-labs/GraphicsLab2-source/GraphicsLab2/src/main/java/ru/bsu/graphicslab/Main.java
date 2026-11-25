package ru.bsu.graphicslab;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Font font = new Font("Segoe UI", Font.PLAIN, 14);
            for (String key : new String[]{"Button", "Label", "Table", "TableHeader", "ProgressBar"}) {
                UIManager.put(key + ".font", font);
            }
            new MainWindow().setVisible(true);
        });
    }
}
