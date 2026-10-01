package ru.university.colormodels.model;

public enum CmykAlgorithm {
    UCR("UCR — удаление подцветного цвета"),
    GCR("GCR — замена серой компоненты");

    private final String displayName;

    CmykAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
