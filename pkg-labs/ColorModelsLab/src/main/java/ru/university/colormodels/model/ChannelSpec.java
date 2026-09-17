package ru.university.colormodels.model;

/** Описание одной координаты цветовой модели для интерфейса. */
public record ChannelSpec(
        String name,
        double min,
        double max,
        double step,
        int decimals,
        String unit) {

    public int sliderScale() {
        return decimals == 0 ? 1 : 10;
    }

    public int sliderMin() {
        return (int) Math.round(min * sliderScale());
    }

    public int sliderMax() {
        return (int) Math.round(max * sliderScale());
    }

    public int toSlider(double value) {
        return (int) Math.round(value * sliderScale());
    }

    public double fromSlider(int value) {
        return (double) value / sliderScale();
    }
}
