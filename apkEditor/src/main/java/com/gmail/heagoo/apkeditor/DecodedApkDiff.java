package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/* loaded from: /tmp/apkx/classes5.dex */
public final class DecodedApkDiff {
    private static void validateArchive(File apk) throws IOException {
        ZipFile zip = new ZipFile(apk);
        try {
            Enumeration<? extends ZipEntry> it = zip.entries();
            Set<String> names = new HashSet<>();
            long total = 0;
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                String n = e.getName();
                if (!n.startsWith("/") && !n.contains("\\")) {
                    if (n.indexOf(0) < 0) {
                        for (String part : n.split("/")) {
                            if (part.equals("..") || part.equals(".")) {
                                throw new IOException("Недопустимый путь в APK: " + n);
                            }
                        }
                        if (!names.add(n)) {
                            throw new IOException("Повторяющаяся запись в APK: " + n);
                        }
                        if (names.size() > 100000) {
                            throw new IOException("Слишком много записей APK");
                        }
                        if (e.getSize() < 0 || e.getSize() > 1073741824) {
                            throw new IOException("Слишком большая или неизвестная запись: " + n);
                        }
                        total += e.getSize();
                        if (total > 4294967296L) {
                            throw new IOException("Превышен лимит распакованных данных APK");
                        }
                    }
                }
                throw new IOException("Недопустимый путь в APK: " + n);
            }
            zip.close();
        } catch (Throwable th) {
            try {
                zip.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:100:0x00c1, code lost:
    
        r0 = "smali_" + r11.substring(0, r11.length() - 4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:102:0x0161, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x00b3, code lost:
    
        r13.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x0175, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0176, code lost:
    
        r1 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x017f, code lost:
    
        if (r13 != null) goto L99;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x0181, code lost:
    
        r13.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:?, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x0185, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0186, code lost:
    
        r1.addSuppressed(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x0189, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x018a, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x0192, code lost:
    
        r12.delete();
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x0195, code lost:
    
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:?, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x00a4, code lost:
    
        throw new java.io.IOException("DEX превышает лимит 512 МБ");
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x00a9, code lost:
    
        r2 = r4;
        r20 = r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x00ae, code lost:
    
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x00b1, code lost:
    
        if (r13 == null) goto L97;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x00bc, code lost:
    
        if (r11.equals("classes.dex") == false) goto L40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x00be, code lost:
    
        r0 = "smali";
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x00df, code lost:
    
        r1 = r0;
        r0 = java.lang.Class.forName("com.gmail.heagoo.apkeditor.pro.DexDecoder").getConstructor(r19).newInstance(r12.getAbsolutePath());
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x0113, code lost:
    
        r0.getClass().getMethod("dex2smali", r19).invoke(r0, new java.io.File(r24, r1).getAbsolutePath());
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x0121, code lost:
    
        r12.delete();
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x0124, code lost:
    
        r4 = r2;
        r0 = r19;
        r5 = r20;
        r2 = r21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x01b8, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01b9, code lost:
    
        r1 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:94:0x012f, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0134, code lost:
    
        r17 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x0160, code lost:
    
        throw new java.io.IOException("DEX " + r11 + ": " + r17.getCause(), r17.getCause());
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x0131, code lost:
    
        r0 = e;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static void decode(Activity activity, File apk, File root, File scratch) throws Exception {
        Throwable th;
        byte[] buf = new byte[32768];
        Class<String> cls = String.class;
        validateArchive(apk);
        final boolean[] success = {false};
        final String[] error = {null};
        ApkParseConsumer consumer = new ApkParseConsumer() { // from class: com.gmail.heagoo.apkeditor.DecodedApkDiff.1
            @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
            public void decodeFailed(String e) {
                error[0] = e;
            }

            @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
            public void decodeProgress(int p) {
            }

            @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
            public void decodeProgressCount(int d, int t) {
            }

            @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
            public void resTableDecoded(boolean b) {
            }

            @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
            public void resourceDecoded(Map<String, String> m) {
                success[0] = true;
            }
        };
        new ApkParseThread(activity, consumer, apk.getAbsolutePath(), root.getAbsolutePath(), true).run();
        if (!success[0]) {
            throw new IOException("Декодирование ресурсов не удалось: " + error[0]);
        }
        ZipFile z = new ZipFile(apk);
        try {
            Enumeration<? extends ZipEntry> it = z.entries();
            Set<String> seen = new HashSet<>();
            loop0: while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                String n = e.getName();
                if (n.matches("classes(?:[2-9]|[1-9][0-9]+)?\\.dex")) {
                    if (!seen.add(n)) {
                        throw new IOException("Повтор DEX: " + n);
                    }
                    try {
                        File tmp = File.createTempFile("dex-", ".dex", scratch);
                        long size = 0;
                        try {
                            InputStream in = z.getInputStream(e);
                            try {
                                OutputStream out = new FileOutputStream(tmp);
                                try {
                                    buf = new byte[32768];
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                while (true) {
                                    Class<String> cls2 = cls;
                                    int k = in.read(buf);
                                    boolean[] success2 = success;
                                    if (k == -1) {
                                        break;
                                    }
                                    String[] error2 = error;
                                    ZipFile z2 = z;
                                    size += k;
                                    if (size > 536870912) {
                                        break loop0;
                                    }
                                    try {
                                        out.write(buf, 0, k);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        try {
                                            out.close();
                                        } catch (Throwable th4) {
                                            th.addSuppressed(th4);
                                        }
                                        throw th;
                                    }
                                }
                            } catch (Throwable th5) {
                                Throwable th6 = th5;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        ZipFile z3 = z;
                        Throwable th9 = th;
                        try {
                            z3.close();
                            throw th9;
                        } catch (Throwable th10) {
                            th9.addSuppressed(th10);
                            throw th9;
                        }
                    }
                }
            }
            z.close();
        } catch (Throwable th11) {
            th = th11;
        }
    }

    private static void pack(File root, File dir, ZipOutputStream z, int[] count) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Нет доступа: " + dir);
        }
        Arrays.sort(files, Comparator.comparing(new Function() { // from class: com.gmail.heagoo.apkeditor.DecodedApkDiff$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((File) obj).getName();
            }
        }));
        for (File f : files) {
            if (Files.isSymbolicLink(f.toPath())) {
                throw new IOException("Символическая ссылка: " + f);
            }
            if (f.isDirectory()) {
                pack(root, f, z, count);
            } else {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".xml") || name.endsWith(".smali")) {
                    int i = count[0] + 1;
                    count[0] = i;
                    if (i > 100000) {
                        throw new IOException("Слишком много декодированных файлов");
                    }
                    String path = root.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
                    z.putNextEntry(new ZipEntry(path));
                    InputStream in = new FileInputStream(f);
                    try {
                        byte[] b = new byte[32768];
                        while (true) {
                            int n = in.read(b);
                            if (n == -1) {
                                break;
                            } else {
                                z.write(b, 0, n);
                            }
                        }
                        in.close();
                        z.closeEntry();
                    } catch (Throwable th) {
                        try {
                            in.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
            }
        }
    }

    public static String compare(Activity activity, File a, File b) throws Exception {
        File cache = activity.getCacheDir();
        File tmp = new File(cache, "decoded-compare-" + UUID.randomUUID());
        if (!tmp.mkdir()) {
            throw new IOException("Не создать временную папку");
        }
        try {
            File one = new File(tmp, "one");
            File two = new File(tmp, "two");
            File za = new File(tmp, "one.zip");
            File zb = new File(tmp, "two.zip");
            decode(activity, a, one, tmp);
            decode(activity, b, two, tmp);
            ZipOutputStream z = new ZipOutputStream(new FileOutputStream(za));
            try {
                pack(one, one, z, new int[]{0});
                z.close();
                z = new ZipOutputStream(new FileOutputStream(zb));
                pack(two, two, z, new int[]{0});
                z.close();
                return "Сравнение декодированных XML и smali. Ресурсы и DEX декодируются существующими инструментами приложения.\\n\\n".replace("\\n", "\n") + ApkCompareCore.compare(za, zb).replace("APK автоматически не декодируется.", "Временные архивы содержат декодированные файлы.").replace("Бинарный XML и DEX не декодируются.", "Сравниваются результаты декодирования.");
            } finally {
            }
        } finally {
            CheckpointFiles.delete(tmp);
        }
    }
}
