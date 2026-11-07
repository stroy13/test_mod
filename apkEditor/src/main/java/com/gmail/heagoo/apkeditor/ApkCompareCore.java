package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.neweditor.Document;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ApkCompareCore {
    private static final int DIFF_CELLS = 1000000;
    private static final long ENTRY_LIMIT = 1073741824;
    private static final int MAX_ENTRIES = 100000;
    private static final int REPORT_LIMIT = 350000;
    private static final int TEXT_LIMIT = 131072;
    private static final long TOTAL_LIMIT = 4294967296L;

    private static class Item {
        final ZipEntry e;
        final byte[] hash;

        Item(ZipEntry e, byte[] h) {
            this.e = e;
            this.hash = h;
        }
    }

    private static class Out {
        StringBuilder b;
        boolean cut;

        private Out() {
            this.b = new StringBuilder();
        }

        void add(String s) {
            if (this.cut) {
                return;
            }
            if (this.b.length() + s.length() <= ApkCompareCore.REPORT_LIMIT) {
                this.b.append(s);
            } else {
                this.b.append("\n… Отчёт обрезан по размеру.\n");
                this.cut = true;
            }
        }

        public String toString() {
            return this.b.toString();
        }
    }

    private static Map<String, Item> read(ZipFile zip) throws Exception {
        Map<String, Item> m = new TreeMap<>();
        Enumeration<? extends ZipEntry> es = zip.entries();
        long total = 0;
        int count = 0;
        byte[] buf = new byte[32768];
        loop0: while (es.hasMoreElements()) {
            ZipEntry e = es.nextElement();
            count++;
            if (count > MAX_ENTRIES) {
                throw new IOException("Слишком много записей ZIP");
            }
            if (!e.isDirectory()) {
                if (m.containsKey(e.getName())) {
                    throw new IOException("Повторяющееся имя в архиве: " + e.getName());
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                long size = 0;
                InputStream in = zip.getInputStream(e);
                while (true) {
                    try {
                        int n = in.read(buf);
                        if (n != -1) {
                            size += n;
                            total += n;
                            if (size > ENTRY_LIMIT || total > TOTAL_LIMIT) {
                                break loop0;
                            }
                            md.update(buf, 0, n);
                        } else {
                            if (in != null) {
                                in.close();
                            }
                            m.put(e.getName(), new Item(e, md.digest()));
                            break;
                        }
                    } catch (Throwable th) {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                }
                throw new IOException("Превышен лимит распакованных данных");
            }
        }
        return m;
    }

    private static String text(ZipFile zip, ZipEntry e) throws IOException {
        if (e.getSize() < 0 || e.getSize() > 131072) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        InputStream in = zip.getInputStream(e);
        while (true) {
            try {
                int n = in.read(buf);
                if (n == -1) {
                    if (in != null) {
                        in.close();
                    }
                    try {
                        String s = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString();
                        for (int i = 0; i < s.length(); i++) {
                            char c = s.charAt(i);
                            if (c < ' ' && c != '\n' && c != '\r' && c != '\t') {
                                return null;
                            }
                        }
                        return s;
                    } catch (CharacterCodingException e2) {
                        return null;
                    }
                }
                if (out.size() + n > TEXT_LIMIT) {
                    if (in != null) {
                        in.close();
                    }
                    return null;
                }
                out.write(buf, 0, n);
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    private static void diff(Out out, String name, String a, String b) {
        String[] x = a.split("\\r?\\n", -1);
        String[] y = b.split("\\r?\\n", -1);
        if ((x.length + 1) * (y.length + 1) > 1000000) {
            out.add("  Построчный diff пропущен: слишком много строк.\n");
            return;
        }
        int[][] d = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, x.length + 1, y.length + 1);
        for (int i = x.length - 1; i >= 0; i--) {
            for (int j = y.length - 1; j >= 0; j--) {
                d[i][j] = x[i].equals(y[j]) ? d[i + 1][j + 1] + 1 : Math.max(d[i + 1][j], d[i][j + 1]);
            }
        }
        out.add("  --- " + name + " (первый)\n  +++ " + name + " (второй)\n");
        int i2 = 0;
        int j2 = 0;
        int changes = 0;
        while (true) {
            if (i2 >= x.length && j2 >= y.length) {
                return;
            }
            if (i2 >= x.length || j2 >= y.length || !x[i2].equals(y[j2])) {
                changes++;
                if (changes <= 400) {
                    if (j2 >= y.length || (i2 != x.length && d[i2][j2 + 1] < d[i2 + 1][j2])) {
                        out.add("  -" + (i2 + 1) + " " + x[i2] + Document.LINE_SEPARATOR_UNIX);
                        i2++;
                    } else {
                        out.add("  +" + (j2 + 1) + " " + y[j2] + Document.LINE_SEPARATOR_UNIX);
                        j2++;
                    }
                } else {
                    out.add("  … Остальные строки пропущены.\n");
                    return;
                }
            } else {
                i2++;
                j2++;
            }
        }
    }

    public static String compare(File first, File second) throws Exception {
        Map<String, Item> am = null;
        TreeSet<String> names;
        StringBuilder sb = null;
        String str = null;
        Out out = new Out();
        out.add("Первый: " + first + "\nВторой: " + second + "\n\nСравнение содержимого ZIP по SHA-256. APK автоматически не декодируется.\n\n");
        ZipFile a = new ZipFile(first);
        try {
            ZipFile b = new ZipFile(second);
            try {
                Map<String, Item> am2 = read(a);
                Map<String, Item> bm = read(b);
                TreeSet<String> names2 = new TreeSet<>(am2.keySet());
                names2.addAll(bm.keySet());
                int added = 0;
                int removed = 0;
                int changed = 0;
                int same = 0;
                List<String> edits = new ArrayList<>();
                Iterator<String> it = names2.iterator();
                while (it.hasNext()) {
                    String n = it.next();
                    Item x = am2.get(n);
                    Item y = bm.get(n);
                    if (x == null) {
                        added++;
                        sb = new StringBuilder();
                        str = "+ ";
                    } else if (y == null) {
                        removed++;
                        sb = new StringBuilder();
                        str = "- ";
                    } else {
                        names = names2;
                        if (Arrays.equals(x.hash, y.hash)) {
                            same++;
                        } else {
                            changed++;
                            out.add("~ " + n + Document.LINE_SEPARATOR_UNIX);
                            edits.add(n);
                        }
                        names2 = names;
                    }
                    if (x == null || y == null) {
                        out.add(sb.append(str).append(n).append(Document.LINE_SEPARATOR_UNIX).toString());
                    }
                    names = names2;
                    names2 = names;
                }
                out.add("\nДобавлено: " + added + "; удалено: " + removed + "; изменено: " + changed + "; без изменений: " + same + ".\n");
                out.add("\nПострочно — только текстовые XML/smali, уже содержащиеся в ZIP как UTF-8. Бинарный XML и DEX не декодируются.\n");
                Iterator<String> it2 = edits.iterator();
                while (it2.hasNext()) {
                    String n2 = it2.next();
                    String l = n2.toLowerCase(Locale.ROOT);
                    if (l.endsWith(".xml") || l.endsWith(".smali")) {
                        String x2 = text(a, am2.get(n2).e);
                        String y2 = text(b, bm.get(n2).e);
                        if (x2 == null) {
                            am = am2;
                        } else if (y2 != null) {
                            diff(out, n2, x2, y2);
                            Map<String, Item> am3 = am2;
                            if (out.cut) {
                                break;
                            }
                            am2 = am3;
                        } else {
                            am = am2;
                        }
                        Iterator<String> it3 = it2;
                        out.add(Document.LINE_SEPARATOR_UNIX + n2 + ": бинарный, не UTF-8 или слишком большой; diff пропущен.\n");
                        am2 = am;
                        it2 = it3;
                    }
                }
                b.close();
                a.close();
                return out.toString();
            } finally {
            }
        } finally {
        }
    }
}
