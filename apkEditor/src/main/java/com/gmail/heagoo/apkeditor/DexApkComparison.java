package com.gmail.heagoo.apkeditor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexApkComparison {
    private static final long MAX_DEX = 536870912;

    private static final class BoundedInput extends FilterInputStream {
        long count;

        BoundedInput(InputStream in) {
            super(in);
        }

        private void add(long n) throws IOException {
            if (n > 0) {
                long j = this.count + n;
                this.count = j;
                if (j > DexApkComparison.MAX_DEX) {
                    throw new IOException("DEX больше 512 МБ");
                }
            }
        }

        @Override
        public int read() throws IOException {
            int b = this.in.read();
            if (b >= 0) {
                add(1L);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int o, int l) throws IOException {
            int n = this.in.read(b, o, l);
            add(n);
            return n;
        }
    }

    private static void invoke(Object obj, String method, Class<?>[] types, Object... args) throws Exception {
        try {
            obj.getClass().getMethod(method, types).invoke(obj, args);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                throw ((Exception) t);
            }
            if (!(t instanceof Error)) {
                throw new IOException(t);
            }
            throw ((Error) t);
        }
    }

    private static void delete(File f) {
        File[] all;
        if (f.isDirectory() && (all = f.listFiles()) != null) {
            for (File c : all) {
                delete(c);
            }
        }
        f.delete();
    }

    private static void merge(File src, File dir, File out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Не прочитать каталог классов: " + dir);
        }
        Arrays.sort(files, Comparator.comparing(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((File) obj).getName();
            }
        }));
        for (File f : files) {
            if (Files.isSymbolicLink(f.toPath())) {
                throw new IOException("Символическая ссылка: " + f);
            }
            if (f.isDirectory()) {
                merge(src, f, out);
            } else if (f.getName().endsWith(".smali")) {
                String rel = src.toPath().relativize(f.toPath()).toString();
                File dest = new File(out, rel);
                if (!dest.getCanonicalFile().toPath().startsWith(out.getCanonicalFile().toPath())) {
                    throw new IOException("Неверное имя класса: " + rel);
                }
                File parent = dest.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Не создать каталог: " + parent);
                }
                if (dest.exists()) {
                    throw new IOException("Повтор класса в нескольких DEX: " + rel);
                }
                Files.move(f.toPath(), dest.toPath(), new CopyOption[0]);
            } else {
                continue;
            }
        }
    }

    public static void decode(File file, File file2, File file3) throws Exception {
        Throwable th;
        ArrayList arrayList = new ArrayList();
        boolean z = false;
        Throwable th2;
        if (!file.isFile() || Files.isSymbolicLink(file.toPath())) {
            throw new IOException("Файл DEX/APK не найден: " + file);
        }
        if (!file2.isDirectory() && !file2.mkdirs()) {
            throw new IOException("Не создать каталог классов");
        }
        Class<?> cls = Class.forName("com.gmail.heagoo.apkeditor.plus.DexDecoder");
        boolean z2 = true;
        int i = 0;
        Constructor<?> constructor = cls.getConstructor(String.class);
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".dex")) {
            if (file.length() > MAX_DEX) {
                throw new IOException("DEX больше 512 МБ");
            }
            File file4 = Files.createTempDirectory(file3.toPath(), "dex-classes-", new FileAttribute[0]).toFile();
            try {
                invoke(constructor.newInstance(file.getAbsolutePath()), "dex2smali", new Class[]{String.class}, file4.getAbsolutePath());
                merge(file4, file4, file2);
                return;
            } finally {
                delete(file4);
            }
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            arrayList = new ArrayList();
            HashSet hashSet = new HashSet();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                try {
                    ZipEntry nextElement = entries.nextElement();
                    String name = nextElement.getName();
                    boolean z3 = z2;
                    if (name.matches("classes(?:[0-9]+)?\\.dex")) {
                        if (!hashSet.add(name)) {
                            throw new IOException("Повтор DEX в APK: " + name);
                        }
                        if (nextElement.getSize() < 0 || nextElement.getSize() > MAX_DEX) {
                            throw new IOException("Размер DEX неизвестен или больше 512 МБ: " + name);
                        }
                        arrayList.add(nextElement);
                    }
                    z2 = z3;
                } catch (Throwable th3) {
                    th = th3;
                    try {
                        zipFile.close();
                        throw th;
                    } catch (Throwable th4) {
                        th.addSuppressed(th4);
                        throw th;
                    }
                }
            }
            z = z2;
        } catch (Throwable th5) {
            th = th5;
        }
        try {
            if (arrayList.isEmpty()) {
                throw new IOException("В APK нет classes*.dex");
            }
            arrayList.sort(Comparator.comparing(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((ZipEntry) obj).getName();
                }
            }));
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                ZipEntry zipEntry = (ZipEntry) it.next();
                File file5 = Files.createTempDirectory(file3.toPath(), "dex-classes-", new FileAttribute[i]).toFile();
                try {
                    int i2 = i;
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new BoundedInput(zipFile.getInputStream(zipEntry)), 65536);
                    try {
                        Object newInstance = constructor.newInstance(file.getAbsolutePath());
                        Iterator it2 = it;
                        Class[] clsArr = new Class[2];
                        clsArr[i2] = InputStream.class;
                        clsArr[z ? 1 : 0] = String.class;
                        Class<?> cls2 = cls;
                        try {
                            invoke(newInstance, "dexStream2smali", clsArr, bufferedInputStream, file5.getAbsolutePath());
                            merge(file5, file5, file2);
                            try {
                                bufferedInputStream.close();
                                i = i2;
                                it = it2;
                                cls = cls2;
                            } catch (Throwable th6) {
                                th = th6;
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th2 = th7;
                            try {
                                bufferedInputStream.close();
                                throw th2;
                            } catch (Throwable th8) {
                                th2.addSuppressed(th8);
                                throw th2;
                            }
                        }
                    } catch (Throwable th9) {
                        th2 = th9;
                    }
                } catch (Throwable th10) {
                    th = th10;
                }
            }
            zipFile.close();
        } catch (Throwable th11) {
            th = th11;
            zipFile.close();
            throw (th instanceof Exception) ? (Exception) th : new Exception(th);
        }
    }

    private DexApkComparison() {
    }
}
