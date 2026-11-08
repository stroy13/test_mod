package com.gmail.heagoo.apkeditor.plus;

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.util.ClassFileNameHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Parallel DEX disassembly with real completed-class progress.
 *
 * <p>Keeps the public API and 128-class progress batching and uses a bounded,
 * explicit executor lifecycle so successful jobs finish cleanly.
 */
public final class FastDexDisassembler {
    public static int workerCount() {
        int cpus = Math.max(1, Runtime.getRuntime().availableProcessors());
        long heap = Runtime.getRuntime().maxMemory();
        int limit = heap < 201326592 ? 2 : heap < 402653184 ? 4 : 8;
        return Math.min(cpus, limit);
    }

    public static void disassemble(DexFile dex, File output, int jobs, BaksmaliOptions options) throws Exception {
        disassemble(dex, output, jobs, options, null);
    }

    public static void disassemble(DexFile dex, File output, int jobs, final BaksmaliOptions options,
                                   final BiConsumer<Integer, Integer> progress) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            throw new IOException("DEX disassembly interrupted");
        }
        final List<ClassDef> classes = new ArrayList<>(dex.getClasses());
        classes.sort(Comparator.comparing(ClassDef::getType));
        final int total = classes.size();
        if (!output.exists() && !output.mkdirs() && !output.isDirectory()) {
            throw new IOException("Can't create the output directory " + output);
        }
        final ClassFileNameHandler names = new ClassFileNameHandler(output, ".smali");
        final AtomicInteger cursor = new AtomicInteger();
        final AtomicInteger done = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Object progressLock = new Object();
        final int[] lastReported = {0};
        int workerTotal = Math.max(1, Math.min(Math.max(1, jobs), Math.max(1, total)));
        ExecutorService pool = Executors.newFixedThreadPool(workerTotal);
        try {
            List<Future<?>> futures = new ArrayList<>(workerTotal);
            for (int worker = 0; worker < workerTotal; worker++) {
                futures.add(pool.submit(() -> {
                    while (failure.get() == null && !Thread.currentThread().isInterrupted()) {
                        int index = cursor.getAndIncrement();
                        if (index >= total) {
                            return;
                        }
                        ClassDef cls = classes.get(index);
                        try {
                            File file;
                            synchronized (names) {
                                file = names.getUniqueFilenameForClass(cls.getType());
                            }
                            File parent = file.getParentFile();
                            if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
                                throw new IOException("Cannot create directory: " + parent);
                            }
                            BaksmaliWriter writer = new BaksmaliWriter(new BufferedWriter(new OutputStreamWriter(
                                    new FileOutputStream(file), StandardCharsets.UTF_8), 16384),
                                    options.implicitReferences ? cls.getType() : null);
                            try {
                                new ClassDefinition(options, cls).writeTo(writer);
                            } catch (Throwable t) {
                                try {
                                    writer.close();
                                } catch (Throwable closeError) {
                                    t.addSuppressed(closeError);
                                }
                                file.delete();
                                throw t;
                            }
                            writer.close();
                            int completed = done.incrementAndGet();
                            if (progress != null && (completed % 128 == 0 || completed == total)) {
                                synchronized (progressLock) {
                                    if (completed > lastReported[0]) {
                                        lastReported[0] = completed;
                                        progress.accept(completed, total);
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            failure.compareAndSet(null, t);
                            return;
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
            Throwable error = failure.get();
            if (error != null) {
                if (error instanceof Error) {
                    throw (Error) error;
                }
                throw new IOException("Could not disassemble DEX class: " + error, error);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("DEX disassembly interrupted", e);
        } finally {
            pool.shutdownNow();
            boolean interrupted = Thread.interrupted();
            try {
                while (!pool.awaitTermination(1L, TimeUnit.DAYS)) {
                    // Wait for interrupted workers to stop touching output files.
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private FastDexDisassembler() {
    }
}
