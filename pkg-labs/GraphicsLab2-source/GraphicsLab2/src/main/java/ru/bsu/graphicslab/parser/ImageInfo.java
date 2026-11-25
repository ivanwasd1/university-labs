package ru.bsu.graphicslab.parser;

public record ImageInfo(String format, long width, long height, Double dpiX, Double dpiY,
                        Integer depth, String compression, String details) {
}
