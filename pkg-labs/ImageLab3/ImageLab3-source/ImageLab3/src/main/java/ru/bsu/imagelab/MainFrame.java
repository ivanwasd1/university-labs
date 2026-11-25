package ru.bsu.imagelab;

import ru.bsu.imagelab.core.Boundary;
import ru.bsu.imagelab.core.ImageBuffer;
import ru.bsu.imagelab.core.Morphology;
import ru.bsu.imagelab.core.ParallelProcessor;
import ru.bsu.imagelab.core.SmoothingFilters;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {
    private static final Color BACKGROUND = new Color(0xF1F5F6);
    private static final Color PANEL_EDGE = new Color(0xDCE4E8);
    private static final Color TEXT = new Color(0x23343D);
    private static final Color MUTED = new Color(0x596D77);
    private static final Color ACCENT = new Color(0x146A68);
    private static final Color INPUT_BARS = new Color(0x52728E);

    private final ParallelProcessor processor = new ParallelProcessor();
    private final JComboBox<Example> examples = new JComboBox<>();
    private final JComboBox<Algorithm> algorithms = new JComboBox<>(Algorithm.values());
    private final JComboBox<Boundary> boundaries = new JComboBox<>(Boundary.values());
    private final JComboBox<Morphology.Shape> shapes = new JComboBox<>(Morphology.Shape.values());
    private final JSpinner size = new JSpinner(new SpinnerNumberModel(5, 1, 63, 2));
    private final JSpinner sigma = new JSpinner(new SpinnerNumberModel(1.2, 0.1, 30.0, 0.1));
    private final JLabel sizeLabel = new JLabel("Размер ядра");
    private final JPanel sigmaRow = setting(new JLabel("σ (сигма)"), sigma);
    private final JPanel shapeRow = setting(new JLabel("Форма элемента"), shapes);
    private final JButton open = button("Открыть файл…", false);
    private final JButton apply = button("Применить", true);
    private final JButton reset = button("Сбросить", false);
    private final JButton save = button("Сохранить PNG…", false);
    private final ImageView before = new ImageView("Откройте изображение или выберите пример");
    private final ImageView after = new ImageView("Примените алгоритм");
    private final HistogramPanel beforeHistogram = new HistogramPanel(INPUT_BARS);
    private final HistogramPanel afterHistogram = new HistogramPanel(ACCENT);
    private final JLabel sourceInfo = new JLabel("Изображение не открыто");
    private final JLabel resultInfo = new JLabel("Ожидает обработки");
    private final JLabel status = new JLabel("Выберите изображение");
    private final JLabel elapsed = new JLabel("Время: —");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final Timer clock = new Timer(120, event -> updateElapsed());
    private ViewData original;
    private ViewData result;
    private boolean busy;
    private boolean changingExample;
    private long started;

    public MainFrame() {
        super("Обработка изображений — лабораторная работа 3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                processor.close();
            }
        });
        setMinimumSize(new Dimension(880, 610));
        setSize(1260, 745);
        setLocationRelativeTo(null);
        buildLayout();
        loadExampleList();
        configureActions();
        updateParameters();
        setBusy(false, false, "");
        progress.setString("0%");
        if (examples.getItemCount() > 0) {
            SwingUtilities.invokeLater(() -> {
                if (original == null && !busy) {
                    examples.setSelectedIndex(0);
                }
            });
        }
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(14, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(17, 19, 15, 19));
        setContentPane(root);

        JPanel header = transparent(new BorderLayout(8, 0));
        JLabel title = new JLabel("Обработка изображений");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);
        JPanel files = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        files.add(open);
        JLabel examplesLabel = new JLabel("Тестовый пример");
        examplesLabel.setForeground(MUTED);
        files.add(examplesLabel);
        examples.setPreferredSize(new Dimension(235, 33));
        files.add(examples);
        header.add(files, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        JPanel settings = new JPanel();
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        settings.setBackground(Color.WHITE);
        settings.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PANEL_EDGE),
                BorderFactory.createEmptyBorder(13, 12, 12, 12)));
        settings.setPreferredSize(new Dimension(218, 0));
        JLabel settingsTitle = new JLabel("Обработка");
        settingsTitle.setFont(settingsTitle.getFont().deriveFont(Font.BOLD, 17f));
        settingsTitle.setForeground(TEXT);
        settingsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        settings.add(settingsTitle);
        settings.add(Box.createVerticalStrut(13));
        settings.add(setting(new JLabel("Алгоритм"), algorithms));
        settings.add(setting(sizeLabel, size));
        settings.add(sigmaRow);
        settings.add(shapeRow);
        settings.add(setting(new JLabel("Границы"), boundaries));
        settings.add(Box.createVerticalStrut(7));
        for (JButton button : new JButton[]{apply, reset, save}) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            settings.add(button);
            settings.add(Box.createVerticalStrut(7));
        }
        settings.add(Box.createVerticalGlue());
        root.add(settings, BorderLayout.WEST);

        JPanel comparison = transparent(new GridLayout(1, 2, 11, 0));
        comparison.add(pane("Исходное", sourceInfo, before, beforeHistogram));
        comparison.add(pane("Результат", resultInfo, after, afterHistogram));
        root.add(comparison, BorderLayout.CENTER);

        JPanel footer = transparent(new BorderLayout(13, 0));
        status.setForeground(MUTED);
        elapsed.setForeground(TEXT);
        elapsed.setPreferredSize(new Dimension(158, 21));
        progress.setForeground(ACCENT);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(180, 19));
        JPanel metrics = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        metrics.add(elapsed);
        metrics.add(progress);
        footer.add(status, BorderLayout.CENTER);
        footer.add(metrics, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setBackground(primary ? ACCENT : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : PANEL_EDGE),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        return button;
    }

    private static JPanel setting(JLabel label, Component field) {
        JPanel row = transparent(new BorderLayout(0, 5));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        label.setForeground(MUTED);
        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JPanel pane(String title, JLabel detail, ImageView view, HistogramPanel histogram) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createLineBorder(PANEL_EDGE));
        JPanel caption = new JPanel(new BorderLayout(0, 4));
        caption.setBackground(Color.WHITE);
        caption.setBorder(BorderFactory.createEmptyBorder(9, 12, 8, 12));
        JLabel label = new JLabel(title);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(12f));
        caption.add(label, BorderLayout.NORTH);
        caption.add(detail, BorderLayout.SOUTH);
        wrapper.add(caption, BorderLayout.NORTH);
        wrapper.add(view, BorderLayout.CENTER);
        wrapper.add(histogram, BorderLayout.SOUTH);
        return wrapper;
    }

    private void configureActions() {
        setLabels(boundaries, new String[]{"Clamp — повтор края", "Reflect — зеркало"});
        setLabels(shapes, new String[]{"Квадрат", "Крест", "Диск"});
        examples.addActionListener(event -> {
            Example sample = (Example) examples.getSelectedItem();
            if (sample != null && !changingExample && !busy) {
                load(() -> readExample(sample.file()), sample.label(), false);
            }
        });
        algorithms.addActionListener(event -> updateParameters());
        open.addActionListener(event -> openFile());
        apply.addActionListener(event -> process());
        reset.addActionListener(event -> resetResult());
        save.addActionListener(event -> saveResult());
    }

    private static <E extends Enum<E>> void setLabels(JComboBox<E> combo, String[] labels) {
        var renderer = combo.getRenderer();
        combo.setRenderer((JList<? extends E> list, E value, int index, boolean selected, boolean focus) -> {
            Component c = renderer.getListCellRendererComponent(list, value, index, selected, focus);
            if (c instanceof JLabel label && value != null) {
                label.setText(labels[value.ordinal()]);
            }
            return c;
        });
    }

    private void loadExampleList() {
        try (InputStream stream = MainFrame.class.getResourceAsStream("/examples/index.txt")) {
            if (stream == null) {
                status.setText("Встроенные примеры не найдены");
                return;
            }
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) {
                    String[] fields = line.split("\\|", 2);
                    if (fields.length == 2) {
                        examples.addItem(new Example(fields[0].trim(), fields[1].trim()));
                    }
                }
            }
            examples.setSelectedIndex(-1);
        } catch (IOException error) {
            status.setText("Не удалось прочитать список примеров");
        }
    }

    private BufferedImage readExample(String name) throws IOException {
        try (InputStream input = MainFrame.class.getResourceAsStream("/examples/" + name)) {
            if (input == null) {
                throw new IOException("Встроенный файл не найден");
            }
            return ImageIO.read(input);
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Открыть изображение");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Изображения (PNG, JPEG, BMP, GIF, TIFF)", "png", "jpg", "jpeg", "bmp", "gif", "tif", "tiff"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            load(() -> ImageIO.read(path.toFile()), path.getFileName().toString(), true);
        }
    }

    private void load(Callable<BufferedImage> reader, String name, boolean external) {
        setBusy(true, true, "Загрузка…");
        status.setText("Открытие изображения");
        new SwingWorker<ViewData, Void>() {
            @Override
            protected ViewData doInBackground() throws Exception {
                BufferedImage image = reader.call();
                if (image == null) {
                    throw new IOException("Формат не поддерживается или файл повреждён");
                }
                return ViewData.of(ImageBuffer.fromImage(image));
            }

            @Override
            protected void done() {
                try {
                    original = get();
                    before.setImage(original.preview());
                    sourceInfo.setText(name + " · " + original.buffer().width() + " × " + original.buffer().height());
                    sourceInfo.setToolTipText(sourceInfo.getText());
                    if (external) {
                        changingExample = true;
                        examples.setSelectedIndex(-1);
                        changingExample = false;
                    }
                    resetResult();
                    status.setText("Изображение открыто");
                } catch (ExecutionException error) {
                    showError("Не удалось открыть изображение", error.getCause());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    setBusy(false, false, "");
                }
            }
        }.execute();
    }

    private void updateParameters() {
        Algorithm selection = (Algorithm) algorithms.getSelectedItem();
        boolean morph = selection != null && selection.operation != null;
        sizeLabel.setText(morph ? "Размер элемента" : "Размер ядра");
        sigmaRow.setVisible(selection == Algorithm.GAUSSIAN);
        shapeRow.setVisible(morph);
        revalidate();
    }

    private void process() {
        if (original == null || busy) {
            return;
        }
        Algorithm algorithm = (Algorithm) algorithms.getSelectedItem();
        int kernel;
        double deviation = 1.0;
        try {
            size.commitEdit();
            kernel = ((Number) size.getValue()).intValue();
            if (kernel % 2 == 0) {
                throw new ParseException("Размер должен быть нечётным, от 1 до 63", 0);
            }
            if (algorithm == Algorithm.GAUSSIAN) {
                sigma.commitEdit();
                deviation = ((Number) sigma.getValue()).doubleValue();
            }
        } catch (ParseException error) {
            showError("Параметры обработки", error);
            return;
        }
        double appliedSigma = deviation;
        Boundary boundary = (Boundary) boundaries.getSelectedItem();
        Morphology.Shape shape = (Morphology.Shape) shapes.getSelectedItem();
        ImageBuffer input = original.buffer();
        setBusy(true, false, "0%");
        progress.setValue(0);
        status.setText("Обработка изображения");
        started = System.nanoTime();
        updateElapsed();
        clock.start();
        SwingWorker<Processed, Void> worker = new SwingWorker<>() {
            @Override
            protected Processed doInBackground() {
                long start = System.nanoTime();
                ImageBuffer output = switch (algorithm) {
                    case BOX -> SmoothingFilters.box(input, kernel, boundary, processor, this::setProgress);
                    case GAUSSIAN -> SmoothingFilters.gaussian(input, kernel, appliedSigma, boundary,
                            processor, this::setProgress);
                    default -> Morphology.apply(input, algorithm.operation, shape, kernel, boundary,
                            processor, this::setProgress);
                };
                long nanos = System.nanoTime() - start;
                return new Processed(ViewData.of(output), nanos);
            }

            @Override
            protected void done() {
                clock.stop();
                try {
                    Processed processed = get();
                    result = processed.data();
                    after.setImage(result.preview());
                    String details = algorithm + " · " + kernel + " × " + kernel;
                    if (algorithm == Algorithm.GAUSSIAN) {
                        details += " · σ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"))
                                .format(appliedSigma);
                    }
                    resultInfo.setText(details);
                    resultInfo.setToolTipText(details + " · " + boundary);
                    updateHistograms();
                    elapsed.setText(formatTime(processed.nanos()));
                    progress.setValue(100);
                    progress.setString("100%");
                    status.setText("Обработка завершена");
                } catch (ExecutionException error) {
                    progress.setValue(0);
                    progress.setString("Ошибка");
                    showError("Не удалось обработать изображение", error.getCause());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    setBusy(false, false, "");
                }
            }
        };
        worker.addPropertyChangeListener(event -> {
            if (busy && "progress".equals(event.getPropertyName())) {
                int percent = (Integer) event.getNewValue();
                progress.setValue(percent);
                progress.setString(percent + "%");
            }
        });
        worker.execute();
    }

    private void resetResult() {
        result = null;
        after.setImage(null);
        resultInfo.setText("Ожидает обработки");
        progress.setValue(0);
        progress.setString("0%");
        elapsed.setText("Время: —");
        resultInfo.setToolTipText(null);
        status.setText("Результат сброшен");
        updateHistograms();
        setBusy(busy, false, "");
    }

    private void saveResult() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить результат");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
        chooser.setSelectedFile(new java.io.File("result.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        Path path = selected.toString().toLowerCase(Locale.ROOT).endsWith(".png")
                ? selected : selected.resolveSibling(selected.getFileName() + ".png");
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this,
                "Заменить файл «" + path.getFileName() + "»?", "Сохранение",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        saveTo(path);
    }

    private void saveTo(Path path) {
        BufferedImage output = result.preview();
        setBusy(true, true, "Сохранение…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                if (!ImageIO.write(output, "png", path.toFile())) {
                    throw new IOException("Не найден кодек PNG");
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    status.setText("Сохранено: " + path.getFileName());
                } catch (ExecutionException error) {
                    showError("Не удалось сохранить результат", error.getCause());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    progress.setString("100%");
                    setBusy(false, false, "");
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy, boolean indeterminate, String text) {
        this.busy = busy;
        open.setEnabled(!busy);
        examples.setEnabled(!busy && examples.getItemCount() != 0);
        algorithms.setEnabled(!busy);
        size.setEnabled(!busy);
        sigma.setEnabled(!busy);
        shapes.setEnabled(!busy);
        boundaries.setEnabled(!busy);
        apply.setEnabled(!busy && original != null);
        reset.setEnabled(!busy && result != null);
        save.setEnabled(!busy && result != null);
        progress.setIndeterminate(indeterminate);
        if (!text.isEmpty()) {
            progress.setString(text);
        }
    }

    private void updateHistograms() {
        int[] a = original == null ? null : original.bins();
        int[] b = result == null ? null : result.bins();
        int upper = Math.max(maxBin(a), maxBin(b));
        beforeHistogram.setHistogram(a, upper);
        afterHistogram.setHistogram(b, upper);
    }

    private static int maxBin(int[] bins) {
        int max = 0;
        if (bins != null) {
            for (int count : bins) {
                max = Math.max(max, count);
            }
        }
        return max;
    }

    private void updateElapsed() {
        elapsed.setText(formatTime(System.nanoTime() - started));
    }

    private static String formatTime(long nanos) {
        double ms = nanos / 1_000_000.0;
        Locale ru = Locale.forLanguageTag("ru-RU");
        return ms < 1000 ? String.format(ru, "Время: %.1f мс", ms)
                : String.format(ru, "Время: %.2f с", ms / 1000.0);
    }

    private void showError(String message, Throwable error) {
        status.setText(message);
        String detail = error.getMessage();
        JOptionPane.showMessageDialog(this, detail == null ? message : detail, message, JOptionPane.ERROR_MESSAGE);
    }

    private record Example(String file, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ViewData(ImageBuffer buffer, BufferedImage preview, int[] bins) {
        static ViewData of(ImageBuffer buffer) {
            return new ViewData(buffer, buffer.toImage(), buffer.histogram());
        }
    }

    private record Processed(ViewData data, long nanos) {
    }

    private enum Algorithm {
        BOX("Усредняющий", null),
        GAUSSIAN("Гауссов", null),
        EROSION("Эрозия", Morphology.Operation.EROSION),
        DILATION("Дилатация", Morphology.Operation.DILATION),
        OPENING("Открытие", Morphology.Operation.OPENING),
        CLOSING("Закрытие", Morphology.Operation.CLOSING),
        GRADIENT("Морфологический градиент", Morphology.Operation.GRADIENT),
        TOP_HAT("Top-Hat", Morphology.Operation.TOP_HAT),
        BLACK_HAT("Black-Hat", Morphology.Operation.BLACK_HAT);

        private final String label;
        private final Morphology.Operation operation;

        Algorithm(String label, Morphology.Operation operation) {
            this.label = label;
            this.operation = operation;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ImageView extends JPanel {
        private final String placeholder;
        private BufferedImage image;

        ImageView(String placeholder) {
            this.placeholder = placeholder;
            setBackground(BACKGROUND);
            setMinimumSize(new Dimension(150, 130));
        }

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) {
                graphics.setColor(MUTED);
                var font = graphics.getFontMetrics();
                graphics.drawString(placeholder, Math.max(6, (getWidth() - font.stringWidth(placeholder)) / 2),
                        getHeight() / 2);
                return;
            }
            int space = 14;
            double scale = Math.min((double) (getWidth() - 2 * space) / image.getWidth(),
                    (double) (getHeight() - 2 * space) / image.getHeight());
            if (scale <= 0.0) {
                return;
            }
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scale > 1
                        ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                        : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            } finally {
                g.dispose();
            }
        }
    }
}
