package ru.bsu.graphicslab;

import ru.bsu.graphicslab.parser.ImageInfo;
import ru.bsu.graphicslab.parser.ImageReader.Result;
import ru.bsu.graphicslab.parser.ImageReader.Status;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

final class MainWindow extends JFrame {
    private static final Color BACKGROUND = new Color(0xf3f5f9);
    private static final Color TEXT = new Color(0x182537);
    private static final Color MUTED = new Color(0x4c5b70);
    private static final Color BLUE = new Color(0x2459be);
    private static final Color WARNING = new Color(0xfff3df);
    private static final Color ERROR_BACKGROUND = new Color(0xffedf0);
    private static final String MISSING = "Не указано";

    private final JFileChooser chooser = new JFileChooser();
    private final JButton filesButton = button("Выбрать файлы", true);
    private final JButton folderButton = button("Выбрать папку", false);
    private final JButton cancelButton = button("Отмена", false);
    private final JLabel summary = new JLabel("Выберите файлы или папку");
    private final JProgressBar progress = new JProgressBar();
    private final ImageTableModel model = new ImageTableModel();
    private final JTable table = createTable();
    private final Timer clock = new Timer(250, event -> updateSummary());
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU"));
    private ScanWorker worker;
    private long started;
    private int total, successful, errors, mismatches;

    MainWindow() {
        super("Лабораторная работа 2 · Метаданные изображений");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(740, 415));
        setSize(1230, 640);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(21, 22, 17, 22));
        setContentPane(content);

        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        JLabel title = new JLabel("Метаданные изображений");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setBorder(new EmptyBorder(0, -8, 0, 0));
        actions.setOpaque(false);
        actions.add(filesButton);
        actions.add(folderButton);
        actions.add(cancelButton);
        header.add(actions, BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xd6deea)));
        content.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        summary.setForeground(MUTED);
        footer.add(summary, BorderLayout.NORTH);
        progress.setStringPainted(true);
        progress.setString("Ожидание");
        progress.setPreferredSize(new Dimension(0, 20));
        progress.setForeground(BLUE);
        footer.add(progress, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);

        filesButton.addActionListener(event -> choose(false));
        folderButton.addActionListener(event -> choose(true));
        cancelButton.addActionListener(event -> {
            if (worker != null) {
                worker.stop.set(true);
                cancelButton.setEnabled(false);
                progress.setString("Остановка…");
            }
        });
        cancelButton.setEnabled(false);
        enableSorting();
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setMargin(new Insets(7, 13, 7, 13));
        if (primary) {
            button.setBackground(BLUE);
            button.setForeground(Color.WHITE);
        }
        return button;
    }

    private JTable createTable() {
        JTable result = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int col = columnAtPoint(event.getPoint());
                if (row < 0 || col < 0) {
                    return null;
                }
                if (convertColumnIndexToModel(col) == 0) {
                    return model.rows.get(convertRowIndexToModel(row)).file().toString();
                }
                Object value = getValueAt(row, col);
                return value == null ? MISSING : value.toString();
            }
        };
        result.setRowHeight(28);
        result.setShowVerticalLines(false);
        result.setGridColor(new Color(0xe9edf4));
        result.setSelectionBackground(new Color(0xd4e4ff));
        result.setSelectionForeground(TEXT);
        result.setFillsViewportHeight(true);
        result.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        result.getTableHeader().setReorderingAllowed(false);
        result.getTableHeader().setPreferredSize(new Dimension(0, 34));
        result.getTableHeader().setToolTipText("Нажмите на заголовок столбца для сортировки после сканирования");
        int[] widths = {142, 85, 87, 87, 87, 87, 89, 145, 135, 216};
        for (int i = 0; i < widths.length; i++) {
            result.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        var renderer = new ResultRenderer();
        result.setDefaultRenderer(Object.class, renderer);
        result.setDefaultRenderer(Number.class, renderer);
        result.setDefaultRenderer(Double.class, renderer);
        return result;
    }

    private void choose(boolean folder) {
        if (worker != null) {
            return;
        }
        chooser.setDialogTitle(folder ? "Выбор папки" : "Выбор графических файлов");
        chooser.setFileSelectionMode(folder ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(!folder);
        chooser.setSelectedFiles(new File[0]);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (folder) {
            startScan(chooser.getSelectedFile().toPath(), List.of());
        } else {
            File[] selected = chooser.getSelectedFiles();
            if (selected.length == 0 && chooser.getSelectedFile() != null) {
                selected = new File[]{chooser.getSelectedFile()};
            }
            startScan(null, Arrays.stream(selected).map(File::toPath).toList());
        }
    }

    private void startScan(Path folder, List<Path> files) {
        table.setRowSorter(null);
        model.clear();
        total = successful = errors = mismatches = 0;
        started = System.nanoTime();
        progress.setValue(0);
        progress.setIndeterminate(true);
        progress.setString("Подготовка списка файлов…");
        filesButton.setEnabled(false);
        folderButton.setEnabled(false);
        cancelButton.setEnabled(true);
        worker = new ScanWorker(folder, files);
        clock.start();
        updateSummary();
        worker.execute();
    }

    private void updateSummary() {
        if (started == 0) {
            return;
        }
        long tenths = (System.nanoTime() - started) / 100_000_000;
        summary.setText("Обработано " + numbers.format(model.getRowCount()) + " из " + numbers.format(total)
                + "     Успешно " + numbers.format(successful)
                + "     Ошибок " + numbers.format(errors)
                + "     Несовпадений " + numbers.format(mismatches)
                + "     Время " + (tenths / 600) + ":" + String.format(Locale.ROOT,
                "%02d,%d", tenths / 10 % 60, tenths % 10));
    }

    private void enableSorting() {
        table.setRowSorter(new TableRowSorter<>(model));
    }

    private record ScanUpdate(int total, List<Result> rows) {
    }

    private final class ScanWorker extends SwingWorker<Void, ScanUpdate> {
        private final Path folder;
        private final List<Path> selected;
        private final AtomicBoolean stop = new AtomicBoolean();

        ScanWorker(Path folder, List<Path> selected) {
            this.folder = folder;
            this.selected = selected;
        }

        @Override
        protected Void doInBackground() throws Exception {
            List<Path> files = folder == null ? selected : FolderScanner.listFiles(folder, stop::get);
            publish(new ScanUpdate(files.size(), List.of()));
            FolderScanner.scan(files, stop::get, rows -> publish(new ScanUpdate(files.size(), rows)));
            return null;
        }

        @Override
        protected void process(List<ScanUpdate> updates) {
            List<Result> added = new ArrayList<>();
            for (ScanUpdate update : updates) {
                total = update.total();
                added.addAll(update.rows());
            }
            for (Result row : added) {
                if (row.successful()) {
                    successful++;
                } else {
                    errors++;
                }
                if (row.status() == Status.WRONG_EXTENSION) {
                    mismatches++;
                }
            }
            model.append(added);
            progress.setIndeterminate(false);
            progress.setMaximum(Math.max(total, 1));
            progress.setValue(model.getRowCount());
            progress.setString(stop.get() ? "Остановка…" : numbers.format(model.getRowCount())
                    + " / " + numbers.format(total));
            updateSummary();
        }

        @Override
        protected void done() {
            clock.stop();
            progress.setIndeterminate(false);
            try {
                get();
                progress.setString(stop.get() ? "Сканирование отменено" :
                        total == 0 ? "В папке нет файлов" : "Сканирование завершено");
            } catch (ExecutionException e) {
                progress.setString("Ошибка сканирования");
                Throwable cause = e.getCause();
                JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(),
                        "Не удалось завершить сканирование", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                progress.setString("Сканирование прервано");
            } finally {
                updateSummary();
                enableSorting();
                worker = null;
                filesButton.setEnabled(true);
                folderButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        }
    }

    private final class ResultRenderer extends DefaultTableCellRenderer {
        private final NumberFormat dpi = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"));

        ResultRenderer() {
            dpi.setMaximumFractionDigits(2);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setBorder(new EmptyBorder(0, 7, 0, 7));
            if (!selected) {
                Status status = model.rows.get(table.convertRowIndexToModel(row)).status();
                setForeground(TEXT);
                setBackground(switch (status) {
                    case OK -> row % 2 == 0 ? Color.WHITE : new Color(0xf6f8fc);
                    case WRONG_EXTENSION -> WARNING;
                    default -> ERROR_BACKGROUND;
                });
            }
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            setText(value == null ? MISSING : value instanceof Double number ? dpi.format(number) : value.toString());
            return this;
        }
    }

    private static final class ImageTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Имя файла", "Формат", "Ширина", "Высота", "DPI X",
                "DPI Y", "Бит/пикс.", "Сжатие", "Детали", "Статус"};
        private final List<Result> rows = new ArrayList<>();

        void clear() {
            rows.clear();
            fireTableDataChanged();
        }

        void append(List<Result> added) {
            if (!added.isEmpty()) {
                int start = rows.size();
                rows.addAll(added);
                fireTableRowsInserted(start, rows.size() - 1);
            }
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 2, 3 -> Long.class;
                case 4, 5 -> Double.class;
                case 6 -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            Result row = rows.get(rowIndex);
            ImageInfo info = row.info();
            return switch (column) {
                case 0 -> row.file().getFileName().toString();
                case 1 -> row.format();
                case 2 -> info == null ? null : info.width();
                case 3 -> info == null ? null : info.height();
                case 4 -> info == null ? null : info.dpiX();
                case 5 -> info == null ? null : info.dpiY();
                case 6 -> info == null ? null : info.depth();
                case 7 -> info == null ? null : info.compression();
                case 8 -> info == null ? null : info.details();
                case 9 -> row.reason().isEmpty() ? row.status().toString() : row.status() + " — " + row.reason();
                default -> throw new IndexOutOfBoundsException(column);
            };
        }
    }
}
