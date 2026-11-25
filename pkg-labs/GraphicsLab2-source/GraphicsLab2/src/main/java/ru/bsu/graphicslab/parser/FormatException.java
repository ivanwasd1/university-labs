package ru.bsu.graphicslab.parser;

import java.io.IOException;

final class FormatException extends IOException {
    FormatException(String message) {
        super(message);
    }
}
