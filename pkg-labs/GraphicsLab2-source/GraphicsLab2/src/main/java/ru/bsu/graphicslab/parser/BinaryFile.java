package ru.bsu.graphicslab.parser;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

final class BinaryFile implements AutoCloseable {
    private final RandomAccessFile file;
    private final long length;
    private final BooleanSupplier cancelled;
    private final byte[] buffer = new byte[4096];
    private long bufferStart = -1;
    private int buffered;
    private long position;

    BinaryFile(Path path) throws IOException {
        this(path, () -> false);
    }

    BinaryFile(Path path, BooleanSupplier cancelled) throws IOException {
        this.cancelled = cancelled;
        file = new RandomAccessFile(path.toFile(), "r");
        try {
            length = file.length();
        } catch (IOException e) {
            file.close();
            throw e;
        }
    }

    long length() { return length; }
    long position() { return position; }

    void require(long offset, long count) throws FormatException {
        if (offset < 0 || count < 0 || offset > length || count > length - offset) {
            throw new FormatException("Участок выходит за границы файла (смещение " + offset + ")");
        }
    }

    void seek(long offset) throws IOException {
        checkCancelled();
        require(offset, 0);
        position = offset;
    }

    void skip(long count) throws IOException {
        checkCancelled();
        require(position, count);
        position += count;
    }

    int u8() throws IOException {
        require(position, 1);
        if (position < bufferStart || position - bufferStart >= buffered) {
            fill();
        }
        return buffer[(int) (position++ - bufferStart)] & 0xff;
    }

    private void fill() throws IOException {
        checkCancelled();
        file.seek(position);
        bufferStart = position;
        buffered = (int) Math.min(buffer.length, length - position);
        file.readFully(buffer, 0, buffered);
    }

    private void checkCancelled() throws InterruptedIOException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Обработка отменена");
        }
    }

    int u16LE() throws IOException { return u8() | (u8() << 8); }
    int u16BE() throws IOException { return (u8() << 8) | u8(); }
    long u32LE() throws IOException { return u16LE() | ((long) u16LE() << 16); }
    long u32BE() throws IOException { return ((long) u16BE() << 16) | u16BE(); }
    int i32LE() throws IOException { return (int) u32LE(); }
    int i32BE() throws IOException { return (int) u32BE(); }

    byte[] bytes(int count) throws IOException {
        require(position, count);
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = (byte) u8();
        }
        return result;
    }

    String ascii(int count) throws IOException {
        return new String(bytes(count), StandardCharsets.US_ASCII);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
