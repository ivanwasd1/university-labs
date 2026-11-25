package ru.bsu.graphicslab;

import ru.bsu.graphicslab.parser.ImageReader;
import ru.bsu.graphicslab.parser.ImageReader.Result;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class FolderScanner {
    private static final int BATCH_SIZE = 32;
    static final int THREADS = Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors()));

    private FolderScanner() {
    }

    static List<Path> listFiles(Path folder, BooleanSupplier cancelled) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(folder, Set.of(), 1, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path entry, BasicFileAttributes attributes) {
                if (cancelled.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }
                if (attributes.isRegularFile()) {
                    files.add(entry);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path entry, IOException error) throws IOException {
                if (entry.equals(folder)) {
                    throw error;
                }
                if (cancelled.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }
                files.add(entry);
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    static void scan(List<Path> files, BooleanSupplier cancelled, Consumer<List<Result>> onBatch)
            throws InterruptedException, ExecutionException {
        scan(files, cancelled, onBatch, THREADS);
    }

    static void scan(List<Path> files, BooleanSupplier cancelled, Consumer<List<Result>> onBatch,
                     int threadCount) throws InterruptedException, ExecutionException {
        if (files.isEmpty() || cancelled.getAsBoolean()) {
            return;
        }
        int batchSize = Math.min(BATCH_SIZE, 1 + (files.size() - 1) / threadCount);
        int workers = Math.min(threadCount, 1 + (files.size() - 1) / batchSize);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            var completed = new ExecutorCompletionService<List<Result>>(pool);
            int next = 0;
            int pending = 0;
            while (pending > 0 || (next < files.size() && !cancelled.getAsBoolean())) {
                while (pending < 2 * workers && next < files.size() && !cancelled.getAsBoolean()) {
                    int start = next;
                    int end = start + Math.min(batchSize, files.size() - start);
                    completed.submit(() -> readBatch(files, start, end, cancelled));
                    next = end;
                    pending++;
                }
                if (pending == 0) {
                    break;
                }
                List<Result> batch = completed.take().get();
                pending--;
                if (!batch.isEmpty()) {
                    onBatch.accept(batch);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<Result> readBatch(List<Path> files, int start, int end, BooleanSupplier cancelled) {
        List<Result> results = new ArrayList<>(end - start);
        for (int i = start; i < end && !cancelled.getAsBoolean(); i++) {
            Result result = ImageReader.read(files.get(i), cancelled);
            if (!cancelled.getAsBoolean()) {
                results.add(result);
            }
        }
        return results;
    }
}
