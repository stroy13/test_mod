package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ProjectCleanup;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

final class ProjectCleanup {

    interface DeleteOperation {
        boolean delete(File file);
    }

    private ProjectCleanup() {
    }

    static boolean deleteAll(File root, BooleanSupplier cancelled) throws IOException {
        return deleteAll(root, cancelled, new DeleteOperation() {
            @Override
            public final boolean delete(File file) {
                return file.delete();
            }
        });
    }

    static boolean deleteAll(File root, BooleanSupplier cancelled, final DeleteOperation operation) throws IOException {
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return !cancelled.getAsBoolean();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors())));
        ArrayDeque<Future<?>> pending = new ArrayDeque<>();
        ArrayDeque<File> stack = new ArrayDeque<>();
        ArrayList<File> directories = new ArrayList<>();
        boolean interrupted = false;
        try {
            stack.push(root);
            while (!stack.isEmpty() && !cancelled.getAsBoolean()) {
                final File file = stack.pop();
                if (!file.isDirectory() || Files.isSymbolicLink(file.toPath())) {
                    pending.add(pool.submit(new Callable() {
                        @Override
                        public final Object call() throws Exception {
                            return ProjectCleanup.lambda$deleteAll$0(operation, file);
                        }
                    }));
                    if (pending.size() >= 24) {
                        waitOne(pending.remove());
                    }
                } else {
                    File[] children = file.listFiles();
                    if (children == null) {
                        throw new IOException("Cannot list old project directory: " + file);
                    }
                    directories.add(file);
                    for (File child : children) {
                        stack.push(child);
                    }
                }
            }
            while (!pending.isEmpty()) {
                waitOne(pending.remove());
            }
            if (cancelled.getAsBoolean()) {
                pool.shutdown();
                while (true) {
                    try {
                        if (pool.awaitTermination(1L, TimeUnit.DAYS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }
            for (int i = directories.size() - 1; i >= 0; i--) {
                if (cancelled.getAsBoolean()) {
                    pool.shutdown();
                    while (true) {
                        try {
                            if (pool.awaitTermination(1L, TimeUnit.DAYS)) {
                                break;
                            }
                        } catch (InterruptedException e2) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                }
                File dir = directories.get(i);
                if (!operation.delete(dir) && Files.exists(dir.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Cannot delete old project directory: " + dir);
                }
            }
            pool.shutdown();
            while (true) {
                try {
                    if (pool.awaitTermination(1L, TimeUnit.DAYS)) {
                        break;
                    }
                } catch (InterruptedException e3) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return true;
        } finally {
            pool.shutdownNow();
        }
    }

    static Object lambda$deleteAll$0(DeleteOperation operation, File file) throws Exception {
        if (!operation.delete(file) && Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Cannot delete old project file: " + file);
        }
        return null;
    }

    private static void waitOne(Future<?> future) throws IOException {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Cleanup interrupted", e);
        } catch (ExecutionException e2) {
            throw new IOException("Old project cleanup failed", e2.getCause());
        }
    }
}
