package ru.bsu.imagelab.core;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Objects;

public final class ImageBuffer {
    private final int width;
    private final int height;
    private final int[] pixels;

    ImageBuffer(int width, int height) {
        if (width < 1 || height < 1 || (long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Недопустимый размер изображения");
        }
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    private ImageBuffer(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        pixels = data(image);
    }

    public static ImageBuffer fromImage(BufferedImage source) {
        Objects.requireNonNull(source, "Нет исходного изображения");
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage argb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argb.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return new ImageBuffer(argb);
    }

    public static ImageBuffer of(int width, int height, int[] argb) {
        Objects.requireNonNull(argb, "Нет массива пикселей");
        if ((long) width * height != argb.length) {
            throw new IllegalArgumentException("Размер массива не соответствует изображению");
        }
        ImageBuffer result = new ImageBuffer(width, height);
        System.arraycopy(argb, 0, result.pixels, 0, argb.length);
        return result;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Алгоритмы читают этот массив. Каждый результат получает собственный буфер. */
    public int[] pixels() {
        return pixels;
    }

    public BufferedImage toImage() {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        System.arraycopy(pixels, 0, data(output), 0, pixels.length);
        return output;
    }

    private static int[] data(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    public static int luminance(int argb) {
        int red = (argb >>> 16) & 255;
        int green = (argb >>> 8) & 255;
        int blue = argb & 255;
        return (299 * red + 587 * green + 114 * blue + 500) / 1000;
    }

    public int[] histogram() {
        int[] bins = new int[256];
        for (int pixel : pixels) {
            bins[luminance(pixel)]++;
        }
        return bins;
    }
}
