package com.gmail.heagoo.apkeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: /tmp/apkx/classes5.dex */
public final class ComparisonCore {
    private static final int MAX_DEPTH = 64;
    private static final long MAX_FILE = 1073741824;
    private static final int MAX_FILES = 100000;
    private static final int MAX_ROWS = 40000;
    private static final long MAX_TOTAL = 4294967296L;
    private static final int TEXT_LIMIT = 4194304;

    public enum Kind {
        FILES,
        ARCHIVES,
        FOLDERS
    }

    public enum Status {
        ADDED,
        REMOVED,
        CHANGED,
        SAME
    }

    public static final class Entry {
        public final boolean directory;
        public final long leftSize;
        public final String name;
        public final long rightSize;
        public final Status status;

        Entry(String n, Status s, Item a, Item b) {
            this.name = n;
            this.status = s;
            this.leftSize = a == null ? -1L : a.size;
            this.rightSize = b != null ? b.size : -1L;
            this.directory = (a != null && a.dir) || (b != null && b.dir);
        }
    }

    public static final class Result {
        public final List<Entry> entries;
        public final Kind kind;
        public final File left;
        public final File right;

        Result(Kind k, File a, File b, List<Entry> e) {
            this.kind = k;
            this.left = a;
            this.right = b;
            this.entries = Collections.unmodifiableList(e);
        }
    }

    public static final class Row {
        public final String left;
        public final int leftNumber;
        public final String right;
        public final int rightNumber;
        public final Status status;

        Row(int l, int r, String a, String b, Status s) {
            this.leftNumber = l;
            this.rightNumber = r;
            this.left = a;
            this.right = b;
            this.status = s;
        }
    }

    public static final class Diff {
        public final boolean approximate;
        public final List<Row> rows;

        Diff(List<Row> r, boolean a) {
            this.rows = r;
            this.approximate = a;
        }
    }

    private static final class Item {
        final boolean dir;
        final byte[] sha;
        final long size;

        Item(byte[] h, long s, boolean d) {
            this.sha = h;
            this.size = s;
            this.dir = d;
        }
    }

    private static final class Budget {
        int entries;
        long total;

        private Budget() {
        }
    }

    private static Item hash(InputStream in, Budget b) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[32768];
        long size = 0;
        while (true) {
            int n = in.read(buf);
            if (n == -1) {
                return new Item(md.digest(), size, false);
            }
            size += n;
            b.total += n;
            if (size > MAX_FILE || b.total > MAX_TOTAL) {
                break;
            }
            md.update(buf, 0, n);
        }
        throw new IOException("Превышен лимит данных сравнения");
    }

    private static void count(Budget b) throws IOException {
        int i = b.entries + 1;
        b.entries = i;
        if (i > MAX_FILES) {
            throw new IOException("Слишком много записей для сравнения");
        }
    }

    private static Map<String, Item> archive(File file) throws Exception {
        Map<String, Item> map = new TreeMap<>();
        Budget budget = new Budget();
        ZipFile zip = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> es = zip.entries();
            while (es.hasMoreElements()) {
                ZipEntry e = es.nextElement();
                count(budget);
                String n = e.getName();
                if (map.containsKey(n)) {
                    throw new IOException("Повторяющееся имя в архиве: " + n);
                }
                if (e.isDirectory()) {
                    map.put(n, new Item(null, 0L, true));
                } else {
                    InputStream in = zip.getInputStream(e);
                    try {
                        map.put(n, hash(in, budget));
                        if (in != null) {
                            in.close();
                        }
                    } finally {
                    }
                }
            }
            zip.close();
            return map;
        } catch (Throwable th) {
            try {
                zip.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void walk(File root, File dir, Map<String, Item> map, Budget budget, int depth) throws Exception {
        if (depth > MAX_DEPTH) {
            throw new IOException("Слишком глубокая папка: " + dir);
        }
        File[] children = dir.listFiles();
        if (children == null) {
            throw new IOException("Нет доступа к папке: " + dir);
        }
        Arrays.sort(children, Comparator.comparing(new Function() {
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((File) obj).getName();
            }
        }));
        for (File f : children) {
            count(budget);
            if (Files.isSymbolicLink(f.toPath())) {
                throw new IOException("Символические ссылки не сравниваются: " + f);
            }
            String rel = root.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
            if (f.isDirectory()) {
                map.put(rel + "/", new Item(null, 0L, true));
                walk(root, f, map, budget, depth + 1);
            } else {
                if (!f.isFile()) {
                    throw new IOException("Необычный тип файла: " + f);
                }
                InputStream in = new FileInputStream(f);
                try {
                    map.put(rel, hash(in, budget));
                    in.close();
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

    private static Map<String, Item> folder(File root) throws Exception {
        if (!root.isDirectory() || Files.isSymbolicLink(root.toPath())) {
            throw new IOException("Не папка или символическая ссылка: " + root);
        }
        Map<String, Item> map = new TreeMap<>();
        walk(root, root, map, new Budget(), 0);
        return map;
    }

    public static Result compare(Kind kind, File a, File b) throws Exception {
        Map<String, Item> x;
        Map<String, Item> x2;
        if (a == null || b == null) {
            throw new IOException("Выберите оба объекта");
        }
        if (kind == Kind.FOLDERS) {
            x = folder(a);
            x2 = folder(b);
        } else if (kind == Kind.ARCHIVES) {
            x = archive(a);
            x2 = archive(b);
        } else {
            Map<String, Item> x3 = new TreeMap<>();
            Map<String, Item> y = new TreeMap<>();
            InputStream in = new FileInputStream(a);
            try {
                x3.put("Файл", hash(in, new Budget()));
                in.close();
                in = new FileInputStream(b);
                y.put("Файл", hash(in, new Budget()));
                in.close();
                x = x3;
                x2 = y;
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        TreeSet<String> all = new TreeSet<>(x.keySet());
        all.addAll(x2.keySet());
        List<Entry> rows = new ArrayList<>();
        Iterator<String> it = all.iterator();
        while (it.hasNext()) {
            String n = it.next();
            Item l = x.get(n);
            Item r = x2.get(n);
            Status st = l == null ? Status.ADDED : r == null ? Status.REMOVED : (l.dir == r.dir && l.size == r.size && Arrays.equals(l.sha, r.sha)) ? Status.SAME : Status.CHANGED;
            rows.add(new Entry(n, st, l, r));
        }
        return new Result(kind, a, b, rows);
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        while (true) {
            int n = in.read(b);
            if (n == -1) {
                return out.toByteArray();
            }
            if (out.size() + n > TEXT_LIMIT) {
                throw new IOException("Текст больше 4 МБ — построчный просмотр недоступен");
            }
            out.write(b, 0, n);
        }
    }

    private static String toText(byte[] bytes) throws IOException {
        try {
            String s = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < ' ' && c != '\n' && c != '\r' && c != '\t') {
                    throw new IOException("Бинарный файл: текстовый diff недоступен");
                }
            }
            return s;
        } catch (CharacterCodingException ex) {
            throw new IOException("Не UTF-8: текстовый diff недоступен", ex);
        }
    }

    public static InputStream openInput(Result result, Entry entry, boolean left) throws IOException {
        if ((left ? entry.leftSize : entry.rightSize) < 0) {
            return null;
        }
        if (entry.directory) {
            throw new IOException("Это папка");
        }
        File root = left ? result.left : result.right;
        if (result.kind == Kind.ARCHIVES) {
            final ZipFile z = new ZipFile(root);
            try {
                ZipEntry e = z.getEntry(entry.name);
                if (e == null || e.isDirectory()) {
                    throw new IOException("Запись не найдена: " + entry.name);
                }
                InputStream in = z.getInputStream(e);
                return new FilterInputStream(in) {
                    @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            z.close();
                        }
                    }
                };
            } catch (IOException | RuntimeException ex) {
                z.close();
                throw ex;
            }
        }
        File source = root;
        if (result.kind == Kind.FOLDERS) {
            for (String part : entry.name.split("/")) {
                if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                    throw new IOException("Недопустимый путь");
                }
                source = new File(source, part);
                if (Files.isSymbolicLink(source.toPath())) {
                    throw new IOException("Символическая ссылка: " + source);
                }
            }
            if (!source.getCanonicalFile().toPath().startsWith(root.getCanonicalFile().toPath())) {
                throw new IOException("Путь вне выбранной папки");
            }
        }
        if (Files.isSymbolicLink(source.toPath())) {
            throw new IOException("Символическая ссылка");
        }
        return new FileInputStream(source);
    }

    public static String readText(Result result, Entry entry, boolean left) throws IOException {
        String text;
        InputStream in = openInput(result, entry, left);
        if (in == null) {
            text = null;
        } else {
            try {
                text = toText(readLimited(in));
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
        if (in != null) {
            in.close();
        }
        return text;
    }

    private static int[] scores(String[] a, int as, int ae, String[] b, int bs, int be, boolean reverse) {
        int m = be - bs;
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int i = 0; i < ae - as; i++) {
            cur[0] = 0;
            String x = a[reverse ? (ae - 1) - i : as + i];
            for (int j = 1; j <= m; j++) {
                String y = b[reverse ? be - j : (bs + j) - 1];
                cur[j] = x.equals(y) ? prev[j - 1] + 1 : Math.max(prev[j], cur[j - 1]);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev;
    }

    private static void matches(String[] a, int as, int ae, String[] b, int bs, int be, List<int[]> out) {
        int i;
        List<int[]> list = out;
        int as2 = as;
        int bs2 = bs;
        while (true) {
            if (as2 >= ae) {
                i = be;
                break;
            }
            i = be;
            if (bs2 >= i || !a[as2].equals(b[bs2])) {
                break;
            }
            list.add(new int[]{as2, bs2});
            as2++;
            bs2++;
        }
        int be2 = i;
        int ae2 = ae;
        while (as2 < ae2 && bs2 < be2 && a[ae2 - 1].equals(b[be2 - 1])) {
            ae2--;
            be2--;
        }
        int ae3 = ae2;
        for (int i2 = 0; i2 < ae - ae3; i2++) {
            list.add(new int[]{ae3 + i2, be2 + i2});
        }
    }

    private static void emitChanged(List<Row> rows, String[] a, int as, int ae, String[] b, int bs, int be) {
        int n = Math.max(ae - as, be - bs);
        for (int k = 0; k < n; k++) {
            int x = as + k < ae ? as + k : -1;
            int y = bs + k < be ? bs + k : -1;
            rows.add(new Row(x + 1, y + 1, x < 0 ? "" : a[x], y >= 0 ? b[y] : "", x < 0 ? Status.ADDED : y < 0 ? Status.REMOVED : Status.CHANGED));
        }
    }

    public static Diff diff(String left, String right) throws IOException {
        String[] a;
        List<int[]> same;
        int lineCount = (left == null ? 0 : 1) + (right == null ? 0 : 1);
        if (left != null) {
            for (int i = 0; i < left.length(); i++) {
                if (left.charAt(i) == '\n' && (lineCount = lineCount + 1) > MAX_ROWS) {
                    throw new IOException("Слишком много строк для просмотра (лимит 40000)");
                }
            }
        }
        if (right != null) {
            for (int i2 = 0; i2 < right.length(); i2++) {
                if (right.charAt(i2) == '\n' && (lineCount = lineCount + 1) > MAX_ROWS) {
                    throw new IOException("Слишком много строк для просмотра (лимит 40000)");
                }
            }
        }
        String[] a2 = left == null ? new String[0] : left.split("\\r?\\n", -1);
        String[] b = right == null ? new String[0] : right.split("\\r?\\n", -1);
        List<int[]> same2 = new ArrayList<>();
        if (a2.length + b.length <= 6000) {
            a = a2;
            matches(a, 0, a2.length, b, 0, b.length, same2);
            b = b;
            same = same2;
        } else {
            a = a2;
            same = same2;
            MyersMatcher.match(a, b, same);
        }
        List<Row> rows = new ArrayList<>();
        int i3 = 0;
        int j = 0;
        for (int[] pair : same) {
            String[] a3 = a;
            emitChanged(rows, a3, i3, pair[0], b, j, pair[1]);
            a = a3;
            rows.add(new Row(pair[0] + 1, pair[1] + 1, a[pair[0]], b[pair[1]], Status.SAME));
            i3 = pair[0] + 1;
            j = pair[1] + 1;
        }
        emitChanged(rows, a, i3, a.length, b, j, b.length);
        return new Diff(rows, false);
    }

    private ComparisonCore() {
    }
}
