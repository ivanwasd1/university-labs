package ru.bsu.imagelab.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;

public final class ParallelProcessor implements AutoCloseable {
    private static final int PARALLEL_THRESHOLD = 65_536;
    private final int workers;
    private final ExecutorService executor;

    public ParallelProcessor() {
        this(Math.min(8, Runtime.getRuntime().availableProcessors()));
    }

    public ParallelProcessor(int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("Число потоков должно быть положительным");
        }
        this.workers = workers;
        executor = workers == 1 ? null : Executors.newFixedThreadPool(workers);
    }

    void forRows(int width, int height, int from, int to, IntConsumer progress, IntConsumer processRow) {
        Progress step = new Progress(height, from, to, progress);
        if (workers == 1 || height == 1 || (long) width * height < PARALLEL_THRESHOLD) {
            runStrip(0, height, step, processRow);
            return;
        }
        int count = Math.min(workers, height);
        List<Callable<Void>> tasks = new ArrayList<>(count);
        for (int strip = 0; strip < count; strip++) {
            int start = (int) ((long) strip * height / count);
            int end = (int) ((long) (strip + 1) * height / count);
            tasks.add(() -> {
                runStrip(start, end, step, processRow);
                return null;
            });
        }
        try {
            // Следующий проход начинает чтение только после завершения всех полос.
            for (Future<Void> result : executor.invokeAll(tasks)) {
                result.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Обработка прервана");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException error) {
                throw error;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Не удалось обработать изображение", e.getCause());
        }
    }

    private static void runStrip(int start, int end, Progress progress, IntConsumer processRow) {
        for (int y = start; y < end; y++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Обработка прервана");
            }
            processRow.accept(y);
            if (progress.consumer != null) {
                progress.advance();
            }
        }
    }

    @Override
    public void close() {
        if (executor != null) {
            for (Runnable task : executor.shutdownNow()) {
                if (task instanceof Future<?> pending) {
                    pending.cancel(true);
                }
            }
        }
    }

    private static final class Progress {
        private final int height;
        private final int from;
        private final int to;
        private final IntConsumer consumer;
        private int rows;
        private int last;

        Progress(int height, int from, int to, IntConsumer consumer) {
            this.height = height;
            this.from = from;
            this.to = to;
            this.consumer = consumer;
            last = from;
        }

        synchronized void advance() {
            int value = from + (int) (++rows * (long) (to - from) / height);
            if (value > last) {
                last = value;
                consumer.accept(value);
            }
        }
    }
}
