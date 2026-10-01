package ru.university.colormodels.model;

public enum ColorModelType {
    RGB,
    LAB,
    CMYK;

    public int channelCount() {
        return this == CMYK ? 4 : 3;
    }
}
