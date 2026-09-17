package ru.university.colormodels.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import ru.university.colormodels.controller.ColorController;
import ru.university.colormodels.model.CmykAlgorithm;
import ru.university.colormodels.model.ColorModelType;
import ru.university.colormodels.model.GamutStrategy;
import ru.university.colormodels.model.Illuminant;

public final class MainFrame extends JFrame implements ColorView {
    private final ColorController controller;
    private final List<ColorPanel> colorPanels = new ArrayList<>();
    private final ColorModelType[] selectedModels = {
            ColorModelType.RGB,
            ColorModelType.LAB,
            ColorModelType.CMYK
    };
    private final JPanel colorSample = new JPanel();
    private final JLabel hexLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private boolean changingModels;

    public MainFrame(ColorController controller) {
        super("Лабораторная работа 1 — Цветовые модели");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 620));
        setSize(1220, 700);
        setLocationRelativeTo(null);
        buildInterface();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setContentPane(root);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Цветовые модели");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(heading);
        top.add(Box.createVerticalStrut(8));

        JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        settings.setAlignmentX(Component.LEFT_ALIGNMENT);
        settings.add(new JLabel("Освещение:"));
        JComboBox<Illuminant> illuminantBox = new JComboBox<>(Illuminant.values());
        illuminantBox.setSelectedItem(controller.getIlluminant());
        illuminantBox.setPreferredSize(new Dimension(190, illuminantBox.getPreferredSize().height));
        illuminantBox.setToolTipText("Матрицы RGB ↔ XYZ будут рассчитаны заново");
        illuminantBox.addActionListener(event -> {
            Illuminant selected = (Illuminant) illuminantBox.getSelectedItem();
            if (selected != null) {
                controller.setIlluminant(selected);
            }
        });
        settings.add(illuminantBox);

        settings.add(Box.createHorizontalStrut(6));
        settings.add(new JLabel("Цветоделение CMYK:"));
        JComboBox<CmykAlgorithm> cmykBox = new JComboBox<>(CmykAlgorithm.values());
        cmykBox.setSelectedItem(controller.getCmykAlgorithm());
        cmykBox.setPreferredSize(new Dimension(245, cmykBox.getPreferredSize().height));
        cmykBox.setToolTipText("UCR работает в тенях, GCR — во всём диапазоне");
        cmykBox.addActionListener(event -> {
            CmykAlgorithm selected = (CmykAlgorithm) cmykBox.getSelectedItem();
            if (selected != null) {
                controller.setCmykAlgorithm(selected);
            }
        });
        settings.add(cmykBox);

        settings.add(Box.createHorizontalStrut(6));
        settings.add(new JLabel("Выход за RGB:"));
        JComboBox<GamutStrategy> gamutBox = new JComboBox<>(GamutStrategy.values());
        gamutBox.setSelectedItem(controller.getGamutStrategy());
        gamutBox.setPreferredSize(new Dimension(205, gamutBox.getPreferredSize().height));
        gamutBox.setToolTipText("Способ вернуть RGB в диапазон 0…255");
        gamutBox.addActionListener(event -> {
            GamutStrategy selected = (GamutStrategy) gamutBox.getSelectedItem();
            if (selected != null) {
                controller.setGamutStrategy(selected);
            }
        });
        settings.add(gamutBox);

        top.add(settings);
        root.add(top, BorderLayout.NORTH);

        JPanel models = new JPanel(new GridLayout(1, 3, 12, 0));
        for (int index = 0; index < selectedModels.length; index++) {
            int panelIndex = index;
            ColorPanel panel = new ColorPanel(
                    controller,
                    selectedModels[index],
                    model -> changeModel(panelIndex, model));
            colorPanels.add(panel);
            models.add(panel);
        }
        JScrollPane scrollPane = new JScrollPane(
                models,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        colorSample.setPreferredSize(new Dimension(120, 66));
        colorSample.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        bottom.add(colorSample, BorderLayout.WEST);

        JPanel information = new JPanel();
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));
        hexLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        statusLabel.setFont(statusLabel.getFont().deriveFont(13f));
        information.add(hexLabel);
        information.add(Box.createVerticalStrut(6));
        information.add(statusLabel);
        bottom.add(information, BorderLayout.CENTER);

        root.add(bottom, BorderLayout.SOUTH);
    }

    private void changeModel(int panelIndex, ColorModelType requestedModel) {
        if (changingModels || requestedModel == selectedModels[panelIndex]) {
            return;
        }
        changingModels = true;
        try {
            ColorModelType previousModel = selectedModels[panelIndex];
            int duplicateIndex = -1;
            for (int index = 0; index < selectedModels.length; index++) {
                if (index != panelIndex && selectedModels[index] == requestedModel) {
                    duplicateIndex = index;
                    break;
                }
            }

            selectedModels[panelIndex] = requestedModel;
            colorPanels.get(panelIndex).applyModel(requestedModel);
            if (duplicateIndex >= 0) {
                selectedModels[duplicateIndex] = previousModel;
                colorPanels.get(duplicateIndex).applyModel(previousModel);
            }
        } finally {
            changingModels = false;
        }
        controller.modelSelectionChanged();
    }

    @Override
    public void refreshFromController() {
        for (int index = 0; index < colorPanels.size(); index++) {
            colorPanels.get(index).setValues(controller.valuesFor(selectedModels[index]));
        }
        colorSample.setBackground(controller.displayColor());
        hexLabel.setText("RGB HEX: " + controller.hexColor());
        statusLabel.setText(controller.getStatus());
        statusLabel.setForeground(controller.hasWarning()
                ? new Color(176, 74, 0)
                : new Color(55, 80, 60));
        repaint();
    }
}
