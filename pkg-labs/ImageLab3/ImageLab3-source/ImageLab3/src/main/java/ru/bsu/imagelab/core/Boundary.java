package ru.bsu.imagelab.core;

public enum Boundary {
    CLAMP,
    REFLECT;

    public int index(int coordinate, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Длина оси должна быть положительной");
        }
        if (coordinate >= 0 && coordinate < length) {
            return coordinate;
        }
        if (this == CLAMP || length == 1) {
            return coordinate < 0 ? 0 : length - 1;
        }
        // Отражение без повторения крайнего образца: -1 -> 1, length -> length - 2.
        long period = 2L * (length - 1);
        long position = Math.floorMod((long) coordinate, period);
        return (int) (position < length ? position : period - position);
    }

    int[] indices(int length, int radius) {
        int[] indices = new int[length + 2 * radius];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = index(i - radius, length);
        }
        return indices;
    }
}
