package ru.university.colormodels.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.DoubleFunction;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/** Рисует под бегунком градиент, вычисляемый для текущих компонент цвета. */
public final class GradientSliderUI extends BasicSliderUI {
    private final DoubleFunction<Color> colorAt;

    public GradientSliderUI(JSlider slider, DoubleFunction<Color> colorAt) {
        super(slider);
        this.colorAt = colorAt;
    }

    @Override
    public void paintTrack(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int x = trackRect.x;
            int width = Math.max(1, trackRect.width);
            int height = 10;
            int y = trackRect.y + (trackRect.height - height) / 2;

            for (int offset = 0; offset < width; offset++) {
                double position = width == 1 ? 0.0 : (double) offset / (width - 1);
                g.setColor(colorAt.apply(position));
                g.drawLine(x + offset, y, x + offset, y + height - 1);
            }
            g.setColor(new Color(90, 90, 90));
            g.drawRoundRect(x, y, width - 1, height - 1, 5, 5);
        } finally {
            g.dispose();
        }
    }
}
