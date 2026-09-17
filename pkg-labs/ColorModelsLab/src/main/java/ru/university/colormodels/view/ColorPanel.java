package ru.university.colormodels.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ru.university.colormodels.controller.ColorController;
import ru.university.colormodels.model.ChannelSpec;
import ru.university.colormodels.model.ColorModelType;

/** Одна из трёх одинаковых панелей выбора и редактирования модели. */
public final class ColorPanel extends JPanel {
    private final ColorController controller;
    private final Consumer<ColorModelType> modelChangeListener;
    private final JComboBox<ColorModelType> modelBox;
    private final JPanel channelsPanel = new JPanel(new GridBagLayout());
    private final List<ChannelControl> controls = new ArrayList<>();
    private ColorModelType model;
    private double[] currentValues;
    private boolean updating;
    private boolean changingModel;

    public ColorPanel(
            ColorController controller,
            ColorModelType initialModel,
            Consumer<ColorModelType> modelChangeListener) {
        this.controller = controller;
        this.model = initialModel;
        this.modelChangeListener = modelChangeListener;
        this.currentValues = new double[initialModel.channelCount()];

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 195, 205)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        setPreferredSize(new Dimension(320, 380));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        JLabel title = new JLabel("Цветовая модель");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        modelBox = new JComboBox<>(ColorModelType.values());
        modelBox.setSelectedItem(initialModel);
        modelBox.setToolTipText("Выберите модель для этой панели");
        modelBox.addActionListener(event -> {
            if (!changingModel) {
                ColorModelType selected = (ColorModelType) modelBox.getSelectedItem();
                if (selected != null && selected != model) {
                    modelChangeListener.accept(selected);
                }
            }
        });
        header.add(title, BorderLayout.WEST);
        header.add(modelBox, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        add(channelsPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton paletteButton = new JButton("Выбрать из палитры…");
        paletteButton.setToolTipText("Открыть палитру выбора цвета");
        paletteButton.addActionListener(event -> chooseFromPalette());
        actions.add(paletteButton);
        add(actions, BorderLayout.SOUTH);

        rebuildControls();
    }

    public ColorModelType getModelType() {
        return model;
    }

    public void applyModel(ColorModelType newModel) {
        changingModel = true;
        try {
            model = newModel;
            modelBox.setSelectedItem(newModel);
            currentValues = new double[newModel.channelCount()];
            rebuildControls();
        } finally {
            changingModel = false;
        }
    }

    public void setValues(double[] values) {
        if (values.length != controls.size()) {
            throw new IllegalArgumentException("Неверное число компонент для " + model);
        }
        updating = true;
        try {
            currentValues = values.clone();
            for (int index = 0; index < controls.size(); index++) {
                ChannelControl control = controls.get(index);
                double value = clamp(values[index], control.spec().min(), control.spec().max());
                control.spinner().setValue(value);
                control.slider().setValue(control.spec().toSlider(value));
                control.slider().repaint();
            }
        } finally {
            updating = false;
        }
    }

    private void rebuildControls() {
        channelsPanel.removeAll();
        controls.clear();
        ChannelSpec[] specs = model.channels();

        for (int index = 0; index < specs.length; index++) {
            ChannelSpec spec = specs[index];
            int channelIndex = index;

            JLabel label = new JLabel(spec.name());
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setPreferredSize(new Dimension(18, label.getPreferredSize().height));

            JSlider slider = new JSlider(spec.sliderMin(), spec.sliderMax());
            slider.setOpaque(false);
            slider.setToolTipText(rangeText(spec));
            slider.setUI(new GradientSliderUI(slider,
                    position -> gradientColor(channelIndex, position)));

            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                    Double.valueOf(spec.min()),
                    Double.valueOf(spec.min()),
                    Double.valueOf(spec.max()),
                    Double.valueOf(spec.step()));
            JSpinner spinner = new JSpinner(spinnerModel);
            String pattern = spec.decimals() == 0 ? "0" : "0.0";
            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, pattern);
            editor.getTextField().setColumns(5);
            spinner.setEditor(editor);
            spinner.setToolTipText("Точный ввод: " + rangeText(spec));

            JLabel unit = new JLabel(spec.unit());
            unit.setPreferredSize(new Dimension(16, unit.getPreferredSize().height));

            ChannelControl control = new ChannelControl(spec, slider, spinner);
            controls.add(control);

            slider.addChangeListener(event -> {
                if (updating) {
                    return;
                }
                double value = spec.fromSlider(slider.getValue());
                updateValue(channelIndex, value, true);
            });
            spinner.addChangeListener(event -> {
                if (updating) {
                    return;
                }
                double value = ((Number) spinner.getValue()).doubleValue();
                updateValue(channelIndex, value, false);
            });

            GridBagConstraints labelConstraints = constraints(0, index);
            labelConstraints.weightx = 0.0;
            channelsPanel.add(label, labelConstraints);

            GridBagConstraints sliderConstraints = constraints(1, index);
            sliderConstraints.weightx = 1.0;
            sliderConstraints.fill = GridBagConstraints.HORIZONTAL;
            channelsPanel.add(slider, sliderConstraints);

            GridBagConstraints spinnerConstraints = constraints(2, index);
            spinnerConstraints.weightx = 0.0;
            channelsPanel.add(spinner, spinnerConstraints);

            GridBagConstraints unitConstraints = constraints(3, index);
            unitConstraints.weightx = 0.0;
            channelsPanel.add(unit, unitConstraints);
        }

        GridBagConstraints filler = constraints(0, specs.length);
        filler.gridwidth = 4;
        filler.weighty = 1.0;
        channelsPanel.add(new JPanel(), filler);
        channelsPanel.revalidate();
        channelsPanel.repaint();
    }

    private void updateValue(int index, double value, boolean fromSlider) {
        ChannelControl control = controls.get(index);
        value = clamp(value, control.spec().min(), control.spec().max());
        currentValues[index] = value;

        updating = true;
        try {
            if (fromSlider) {
                control.spinner().setValue(value);
            } else {
                control.slider().setValue(control.spec().toSlider(value));
            }
        } finally {
            updating = false;
        }
        controller.updateFrom(model, currentValues);
    }

    private Color gradientColor(int channelIndex, double position) {
        double[] sample = currentValues.clone();
        if (channelIndex >= sample.length) {
            return Color.GRAY;
        }
        ChannelSpec spec = controls.get(channelIndex).spec();
        sample[channelIndex] = spec.min() + position * (spec.max() - spec.min());
        return controller.preview(model, sample);
    }

    private void chooseFromPalette() {
        Color selected = JColorChooser.showDialog(
                this,
                "Выбор цвета — " + model,
                controller.displayColor());
        if (selected != null) {
            controller.updateFromPalette(selected);
        }
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(8, column == 0 ? 0 : 5, 8, 0);
        return constraints;
    }

    private static String rangeText(ChannelSpec spec) {
        return spec.name() + ": " + number(spec.min(), spec.decimals())
                + "…" + number(spec.max(), spec.decimals()) + spec.unit();
    }

    private static String number(double value, int decimals) {
        return decimals == 0
                ? Long.toString(Math.round(value))
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ChannelControl(ChannelSpec spec, JSlider slider, JSpinner spinner) {
    }
}
