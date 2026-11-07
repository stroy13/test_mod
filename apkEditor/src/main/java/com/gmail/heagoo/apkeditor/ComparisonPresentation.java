package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ComparisonCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ComparisonPresentation {

    public enum Filter {
        DIFFERENCES,
        CHANGED,
        ADDED,
        REMOVED,
        SAME,
        ALL
    }

    public static int[] counts(List<ComparisonCore.Entry> entries) {
        int[] c = new int[4];
        for (ComparisonCore.Entry e : entries) {
            int index = index(e.status);
            c[index] = c[index] + 1;
        }
        return c;
    }

    private static int index(ComparisonCore.Status s) {
        switch (s) {
            case CHANGED:
                return 0;
            case ADDED:
                return 1;
            case REMOVED:
                return 2;
            default:
                return 3;
        }
    }

    public static List<ComparisonCore.Entry> filter(List<ComparisonCore.Entry> entries, Filter filter, String search) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<List<ComparisonCore.Entry>> buckets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            buckets.add(new ArrayList<>());
        }
        for (ComparisonCore.Entry e : entries) {
            int k = index(e.status);
            boolean show = true;
            if (filter != Filter.ALL && ((filter != Filter.DIFFERENCES || k == 3) && ((filter != Filter.CHANGED || k != 0) && ((filter != Filter.ADDED || k != 1) && ((filter != Filter.REMOVED || k != 2) && (filter != Filter.SAME || k != 3)))))) {
                show = false;
            }
            if (show && (term.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(term))) {
                buckets.get(k).add(e);
            }
        }
        List<ComparisonCore.Entry> visible = new ArrayList<>();
        for (List<ComparisonCore.Entry> bucket : buckets) {
            visible.addAll(bucket);
        }
        return visible;
    }

    public static String status(ComparisonCore.Entry e) {
        switch (e.status) {
            case CHANGED:
                return e.directory ? "ИЗМЕНЕНА • папка" : "ИЗМЕНЁН / ЗАМЕНЁН • содержимое отличается";
            case ADDED:
                return e.directory ? "ДОБАВЛЕНА • только во втором" : "ДОБАВЛЕН • только во втором";
            case REMOVED:
                return e.directory ? "УДАЛЕНА • была только в первом" : "УДАЛЁН • был только в первом";
            default:
                return "БЕЗ ИЗМЕНЕНИЙ";
        }
    }

    public static String name(ComparisonCore.Entry e) {
        String path = e.name;
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        int slash = path.lastIndexOf(47);
        return path.substring(slash + 1);
    }

    public static String parent(ComparisonCore.Entry e) {
        String path = e.name;
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        int slash = path.lastIndexOf(47);
        return slash < 0 ? "В корне" : path.substring(0, slash) + "/";
    }

    public static String size(long n) {
        return n < 0 ? "нет" : n < 1024 ? n + " Б" : n < 1048576 ? String.format(Locale.ROOT, "%.1f КБ", Double.valueOf(n / 1024.0d)) : n < 1073741824 ? String.format(Locale.ROOT, "%.2f МБ", Double.valueOf(n / 1048576.0d)) : String.format(Locale.ROOT, "%.2f ГБ", Double.valueOf(n / 1.073741824E9d));
    }

    public static String detail(ComparisonCore.Entry e) {
        return e.directory ? "Папка" : "Первый: " + size(e.leftSize) + "  →  второй: " + size(e.rightSize);
    }

    private ComparisonPresentation() {
    }
}
