package ru.university.colormodels.controller;

import java.awt.Color;

import ru.university.colormodels.model.CmykAlgorithm;
import ru.university.colormodels.model.ColorConverter;
import ru.university.colormodels.model.ColorModelType;
import ru.university.colormodels.model.ConversionOutcome;
import ru.university.colormodels.model.GamutMapper;
import ru.university.colormodels.model.GamutStrategy;
import ru.university.colormodels.model.Illuminant;
import ru.university.colormodels.view.ColorView;

/** Состояние текущего цвета и обработка изменений из интерфейса. */
public final class ColorController {
    private final ColorConverter converter;
    private double[] rgb = {52.0, 132.0, 214.0};
    private double[] xyz;
    private CmykAlgorithm cmykAlgorithm = CmykAlgorithm.GCR;
    private GamutStrategy gamutStrategy = GamutStrategy.CLIPPING;
    private ColorModelType lastSource;
    private double[] lastSourceValues;
    private String status = "Значения в полях округляются только для отображения.";
    private boolean warning;
    private ColorView view;

    public ColorController(ColorConverter converter) {
        this.converter = converter;
        this.xyz = converter.rgbToXyz(rgb);
    }

    public void attachView(ColorView view) {
        this.view = view;
    }

    public void updateFrom(ColorModelType source, double[] values) {
        ConversionOutcome outcome;
        if (source == ColorModelType.LAB) {
            xyz = converter.labToXyz(values);
            outcome = GamutMapper.map(
                    converter.xyzToRgbRaw(xyz), gamutStrategy);
        } else {
            outcome = converter.toRgb(source, values, gamutStrategy);
            xyz = converter.rgbToXyz(outcome.rgb());
        }
        rgb = outcome.rgb();
        lastSource = source;
        lastSourceValues = values.clone();
        setOutcomeStatus(outcome);
        refreshView();
    }

    public void updateFromPalette(Color color) {
        rgb = new double[] {color.getRed(), color.getGreen(), color.getBlue()};
        xyz = converter.rgbToXyz(rgb);
        lastSource = ColorModelType.RGB;
        lastSourceValues = rgb.clone();
        status = "Цвет выбран из палитры.";
        warning = false;
        refreshView();
    }

    public double[] valuesFor(ColorModelType model) {
        if (model == lastSource && lastSourceValues != null) {
            return lastSourceValues.clone();
        }
        if (model == ColorModelType.LAB) {
            return converter.xyzToLab(xyz);
        }
        return converter.fromRgb(model, rgb, cmykAlgorithm);
    }

    /** Цвет одной точки градиента; состояние приложения при этом не меняется. */
    public Color preview(ColorModelType model, double[] values) {
        ConversionOutcome outcome = converter.toRgb(
                model, values, gamutStrategy);
        return toAwtColor(outcome.rgb());
    }

    public Color displayColor() {
        return toAwtColor(rgb);
    }

    public void setIlluminant(Illuminant illuminant) {
        if (converter.getIlluminant() == illuminant) {
            return;
        }
        converter.setIlluminant(illuminant);
        xyz = converter.rgbToXyz(rgb);
        forgetSource();
        status = "Матрицы RGB ↔ XYZ пересчитаны для " + illuminant.name() + ".";
        warning = false;
        refreshView();
    }

    public void setCmykAlgorithm(CmykAlgorithm algorithm) {
        if (this.cmykAlgorithm == algorithm) {
            return;
        }
        this.cmykAlgorithm = algorithm;
        forgetSource();
        status = "Алгоритм цветоделения: " + algorithm.name() + ".";
        warning = false;
        refreshView();
    }

    public void setGamutStrategy(GamutStrategy strategy) {
        if (this.gamutStrategy == strategy) {
            return;
        }
        this.gamutStrategy = strategy;
        if (lastSource == ColorModelType.LAB && lastSourceValues != null) {
            ConversionOutcome outcome = GamutMapper.map(
                    converter.xyzToRgbRaw(xyz), gamutStrategy);
            rgb = outcome.rgb();
            setOutcomeStatus(outcome);
        } else {
            status = "Обработка выхода из RGB: " + strategy.name() + ".";
            warning = false;
        }
        refreshView();
    }

    public Illuminant getIlluminant() {
        return converter.getIlluminant();
    }

    public CmykAlgorithm getCmykAlgorithm() {
        return cmykAlgorithm;
    }

    public GamutStrategy getGamutStrategy() {
        return gamutStrategy;
    }

    public String getStatus() {
        return status;
    }

    public boolean hasWarning() {
        return warning;
    }

    private void forgetSource() {
        lastSource = null;
        lastSourceValues = null;
    }

    private void setOutcomeStatus(ConversionOutcome outcome) {
        warning = outcome.outOfGamut();
        if (warning) {
            status = gamutStrategy == GamutStrategy.CLIPPING
                    ? "Цвет вышел за RGB 0…255: применено обрезание."
                    : "Цвет вышел за RGB 0…255: применено масштабирование.";
        } else {
            status = "Все представления цвета пересчитаны автоматически.";
        }
    }

    private void refreshView() {
        if (view != null) {
            view.refreshFromController();
        }
    }

    private static Color toAwtColor(double[] rgb) {
        int red = (int) Math.round(clamp(rgb[0], 0.0, 255.0));
        int green = (int) Math.round(clamp(rgb[1], 0.0, 255.0));
        int blue = (int) Math.round(clamp(rgb[2], 0.0, 255.0));
        return new Color(red, green, blue);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
