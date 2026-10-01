package ru.university.colormodels;

import java.awt.EventQueue;

import javax.swing.UIManager;

import ru.university.colormodels.controller.ColorController;
import ru.university.colormodels.model.ColorConverter;
import ru.university.colormodels.model.Illuminant;
import ru.university.colormodels.view.MainFrame;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Стандартный Swing LookAndFeel тоже подходит.
            }

            ColorConverter converter = new ColorConverter(Illuminant.D65);
            ColorController controller = new ColorController(converter);
            MainFrame frame = new MainFrame(controller);
            controller.attachView(frame);
            frame.refreshFromController();
            frame.setVisible(true);
        });
    }
}
