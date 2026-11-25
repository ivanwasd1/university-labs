package ru.bsu.imagelab;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.Locale;

final class HistogramPanel extends JPanel {
    private static final Color AXIS = new Color(0x6B7983);
    private static final Color GRID = new Color(0xE4EAED);
    private final Color bars;
    private int[] bins;
    private int scale = 1;

    HistogramPanel(Color bars) {
        this.bars = bars;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(300, 147));
        setMinimumSize(new Dimension(160, 116));
        setToolTipText("Яркость от 0 до 255. Вертикальная шкала общая для обеих гистограмм.");
    }

    void setHistogram(int[] bins, int scale) {
        this.bins = bins;
        this.scale = Math.max(1, scale);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String topCount = NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(scale);
            int left = Math.max(39, g.getFontMetrics().stringWidth(topCount) + 14);
            int right = getWidth() - 18;
            int top = 30;
            int bottom = getHeight() - 24;
            if (right <= left || bottom <= top) {
                return;
            }
            g.setColor(AXIS);
            g.drawString("Гистограмма яркости", 13, 19);
            String yLabel = "пикселей";
            g.drawString(yLabel, right - g.getFontMetrics().stringWidth(yLabel), 19);
            int plotWidth = right - left;
            int plotHeight = bottom - top;
            g.setColor(GRID);
            g.drawLine(left, top, right, top);
            g.drawLine(left, top + plotHeight / 2, right, top + plotHeight / 2);
            if (bins != null) {
                g.setColor(bars);
                for (int i = 0; i < bins.length; i++) {
                    if (bins[i] == 0) {
                        continue;
                    }
                    int x = left + 1 + i * (plotWidth - 1) / 256;
                    int width = left + 1 + (i + 1) * (plotWidth - 1) / 256 - x;
                    int height = Math.max(1, (int) Math.round((double) bins[i] * plotHeight / scale));
                    g.fillRect(x, bottom - height, Math.max(1, width), height);
                }
            }
            g.setColor(AXIS);
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            g.drawString(topCount, left - 7 - g.getFontMetrics().stringWidth(topCount), top + 4);
            g.drawString("0", left - 7 - g.getFontMetrics().stringWidth("0"), bottom);
            for (int tick : new int[]{0, 64, 128, 192, 255}) {
                int x = left + tick * plotWidth / 255;
                String text = Integer.toString(tick);
                g.drawLine(x, bottom, x, bottom + 3);
                g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, bottom + 18);
            }
        } finally {
            g.dispose();
        }
    }
}
