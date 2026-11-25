package ru.bsu.imagelab;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Font font = new Font("Segoe UI", Font.PLAIN, 14);
            for (String component : new String[]{"Button", "Label", "ComboBox", "Spinner", "ProgressBar"}) {
                UIManager.put(component + ".font", font);
            }
            UIManager.getLookAndFeelDefaults().remove("ComboBox.gradient");
            new MainFrame().setVisible(true);
        });
    }
}
