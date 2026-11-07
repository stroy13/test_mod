package com.gmail.heagoo.apkeditor.pro;

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.util.ClassFileNameHandler;
import com.gmail.heagoo.apkeditor.pro.FastDexDisassembler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

/* loaded from: /tmp/apkx/classes26.dex */
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

    /* JADX WARN: Can't wrap try/catch for region: R(8:(1:80)|81|67|68|69|71|70|73) */
    /* JADX WARN: Removed duplicated region for block: B:77:0x0220  */
    /* JADX WARN: Removed duplicated region for block: B:79:? A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0214 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void disassemble(DexFile dex, File output, int jobs, final BaksmaliOptions options, final BiConsumer<Integer, Integer> progress) throws Exception {
        final File[] planned;
        ExecutorService pool;
        final AtomicReference<Throwable> failure;
        String str = null;
        ExecutorService pool2;
        Throwable interruptedException;
        Exception e = null;
        final List<ClassDef> classes = new ArrayList<>(dex.getClasses());
        final String[] types = new String[classes.size()];
        boolean cachedSort = dex instanceof DexBackedDexFile;
        NamedClass[] sorted = cachedSort ? new NamedClass[classes.size()] : null;
        int i = 0;
        boolean cachedSort2 = cachedSort;
        while (true) {
            String str2 = "DEX disassembly interrupted";
            if (i >= types.length) {
                if (cachedSort2) {
                    Arrays.sort(sorted, Comparator.comparing(new Function() { // from class: com.gmail.heagoo.apkeditor.pro.FastDexDisassembler$$ExternalSyntheticLambda1
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String str3;
                            str3 = ((FastDexDisassembler.NamedClass) obj).type;
                            return str3;
                        }
                    }));
                    for (int i2 = 0; i2 < sorted.length; i2++) {
                        classes.set(i2, sorted[i2].cls);
                        types[i2] = sorted[i2].type;
                    }
                } else {
                    Collections.sort(classes);
                    for (int i3 = 0; i3 < types.length; i3++) {
                        types[i3] = classes.get(i3).getType();
                    }
                }
                final boolean fast = FastClassPaths.canUse(output, types);
                if (fast) {
                    FastClassPaths paths = new FastClassPaths(output);
                    planned = new File[types.length];
                    for (int i4 = 0; i4 < types.length; i4++) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new IOException("DEX disassembly interrupted");
                        }
                        planned[i4] = paths.get(types[i4]);
                    }
                } else {
                    planned = null;
                }
                final ClassFileNameHandler names = fast ? null : new ClassFileNameHandler(output, ".smali");
                final ConcurrentHashMap<File, Boolean> prepared = new ConcurrentHashMap<>();
                AtomicReference<Throwable> failure2 = new AtomicReference<>();
                AtomicInteger cursor = new AtomicInteger();
                int[] legacyCursor = {0};
                final AtomicInteger done = new AtomicInteger();
                final Object progressLock = new Object();
                final int[] lastReported = {0};
                int count = Math.max(1, Math.min(Math.max(1, jobs), Math.max(1, classes.size())));
                ExecutorService pool3 = Executors.newFixedThreadPool(count);
                List<Future<?>> futures = new ArrayList<>(count);
                boolean interrupted = false;
                int worker = 0;
                while (true) {
                    pool = pool3;
                    failure = failure2;
                    if (worker >= count) {
                        break;
                    }
                    try {
                        int count2 = count;
                        NamedClass[] sorted2 = sorted;
                        str = str2;
                        final AtomicInteger cursor2 = cursor;
                        final int[] legacyCursor2 = legacyCursor;
                        try {
                            pool2 = pool;
                            try {
                                List<Future<?>> futures2 = futures;
                                try {
                                    try {
                                        futures2.add(pool2.submit(new Runnable() { // from class: com.gmail.heagoo.apkeditor.pro.FastDexDisassembler$$ExternalSyntheticLambda2
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                FastDexDisassembler.lambda$disassemble$2(failure, fast, cursor2, classes, planned, names, legacyCursor2, types, prepared, options, done, progress, progressLock, lastReported);
                                            }
                                        }));
                                        worker++;
                                        failure2 = failure;
                                        pool3 = pool2;
                                        futures = futures2;
                                        count = count2;
                                        sorted = sorted2;
                                        legacyCursor = legacyCursor2;
                                        cursor = cursor2;
                                        str2 = str;
                                    } catch (Exception e2) {
                                        e = e2;
                                    }
                                } catch (Throwable e3) {
                                    interruptedException = e3;
                                    pool2.shutdownNow();
                                    while (true) {
                                        try {
                                            if (pool2.awaitTermination(1L, TimeUnit.DAYS)) {
                                                break;
                                            }
                                        } catch (InterruptedException e4) {
                                            interrupted = true;
                                        }
                                    }
                                    if (interrupted) {
                                        throw interruptedException;
                                    }
                                    Thread.currentThread().interrupt();
                                    throw interruptedException;
                                }
                            } catch (InterruptedException e5) {
                                e = e5;
                            } catch (Throwable th) {
                                interruptedException = th;
                                pool2.shutdownNow();
                                try {
                                    pool2.awaitTermination(1L, TimeUnit.DAYS);
                                } catch (InterruptedException ignored) {
                                }
                                if (interrupted) {
                                }
                            }
                        } catch (Exception e6) {
                            e = e6;
                            pool2 = pool;
                        } catch (Throwable th2) {
                            pool2 = pool;
                            interruptedException = th2;
                        }
                    } catch (Exception e7) {
                        e = e7;
                        str = str2;
                        pool2 = pool;
                    } catch (Throwable th3) {
                        pool2 = pool;
                        interruptedException = th3;
                    }
                    interrupted = true;
                    throw new IOException(str, e);
                }
                str = str2;
                pool2 = pool;
                for (Future<?> future : futures) {
                    future.get();
                }
                Throwable error = failure.get();
                if (error != null) {
                    if (error instanceof Error) {
                        throw ((Error) error);
                    }
                    throw new IOException("Could not disassemble DEX class: " + error, error);
                }
                pool2.shutdownNow();
                while (!pool2.awaitTermination(1L, TimeUnit.DAYS)) {
                    try {
                    } catch (Exception e8) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                return;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("DEX disassembly interrupted");
            }
            types[i] = classes.get(i).getType();
            if (cachedSort2) {
                if (validForSort(types[i])) {
                    sorted[i] = new NamedClass(classes.get(i), types[i]);
                } else {
                    cachedSort2 = false;
                }
            }
            i++;
        }
    }

    static /* synthetic */ void lambda$disassemble$2(AtomicReference failure, boolean fast, AtomicInteger cursor, List classes, File[] planned, ClassFileNameHandler names, int[] legacyCursor, String[] types, ConcurrentHashMap prepared, BaksmaliOptions options, AtomicInteger done, BiConsumer progress, Object progressLock, int[] lastReported) {
        ClassDef cls;
        File file;
        while (failure.get() == null && !Thread.currentThread().isInterrupted()) {
            if (fast) {
                try {
                    int index = cursor.getAndIncrement();
                    if (index >= classes.size()) {
                        return;
                    }
                    cls = (ClassDef) classes.get(index);
                    file = planned[index];
                } catch (Throwable th) {
                    failure.compareAndSet(null, th);
                    return;
                }
            } else {
                synchronized (names) {
                    try {
                        if (legacyCursor[0] >= classes.size()) {
                            return;
                        }
                        cls = (ClassDef) classes.get(legacyCursor[0]);
                        int i = legacyCursor[0];
                        legacyCursor[0] = i + 1;
                        file = names.getUniqueFilenameForClass(types[i]);
                    } catch (Throwable th2) {
                        failure.compareAndSet(null, th2);
                        return;
                    }
                }
            }
            File parent = file.getParentFile();
            try {
                prepared.computeIfAbsent(parent, new Function() { // from class: com.gmail.heagoo.apkeditor.pro.FastDexDisassembler$$ExternalSyntheticLambda3
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return FastDexDisassembler.lambda$disassemble$1((File) obj);
                    }
                });
                try {
                    BaksmaliWriter w = new BaksmaliWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), 16384), options.implicitReferences ? cls.getType() : null);
                    try {
                        new ClassDefinition(options, cls).writeTo(w);
                        w.close();
                        int n = done.incrementAndGet();
                        if (progress != null && (n % 128 == 0 || n == classes.size())) {
                            synchronized (progressLock) {
                                if (n > lastReported[0]) {
                                    lastReported[0] = n;
                                    progress.accept(Integer.valueOf(n), Integer.valueOf(classes.size()));
                                }
                            }
                        }
                    } catch (Throwable th4) {
                        try {
                            w.close();
                            throw th4;
                        } catch (Throwable th5) {
                            th4.addSuppressed(th5);
                            throw th4;
                        }
                    }
                } catch (Exception e) {
                    file.delete();
                    throw e;
                }
            } catch (Throwable th6) {
                failure.compareAndSet(null, th6);
                return;
            }
        }
    }

    static /* synthetic */ Boolean lambda$disassemble$1(File p) {
        if (p.isDirectory() || p.mkdirs() || p.isDirectory()) {
            return Boolean.TRUE;
        }
        throw new IllegalStateException("Cannot create directory: " + p);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class NamedClass {
        final ClassDef cls;
        final String type;

        NamedClass(ClassDef cls, String type) {
            this.cls = cls;
            this.type = type;
        }
    }

    private static boolean validForSort(String s) {
        if (s == null || s.length() < 3 || s.charAt(0) != 'L' || s.charAt(s.length() - 1) != ';') {
            return false;
        }
        boolean segment = false;
        for (int i = 1; i < s.length() - 1; i++) {
            char c = s.charAt(i);
            if (c != ';') {
                if (c != '/') {
                    segment = true;
                } else if (segment) {
                    segment = false;
                }
            }
            return false;
        }
        return segment;
    }

    private FastDexDisassembler() {
    }
}
