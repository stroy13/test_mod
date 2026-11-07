package com.gmail.heagoo.apkeditor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public final class RecentHistory {
    public static final int LIMIT = 5;

    public static final class Entry {
        public final String packageName;
        public final String path;

        public Entry(String p, String pkg) {
            this.path = p;
            this.packageName = pkg == null ? "" : pkg;
        }
    }

    private RecentHistory() {
    }

    public static List<Entry> parse(String data) {
        List<Entry> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (String line : data.split("\\n")) {
            if (result.size() >= 5) {
                break;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length == 2) {
                try {
                    String path = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                    String pkg = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    if (!path.isEmpty()) {
                        result.add(new Entry(path, pkg));
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return result;
    }

    public static String serialize(List<Entry> entries) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Entry e : entries) {
            if (e != null && e.path != null && !e.path.isEmpty()) {
                int count2 = count + 1;
                if (count >= 5) {
                    break;
                }
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(Base64.getEncoder().encodeToString(e.path.getBytes(StandardCharsets.UTF_8))).append('|').append(Base64.getEncoder().encodeToString(e.packageName.getBytes(StandardCharsets.UTF_8)));
                count = count2;
            }
        }
        return out.toString();
    }

    public static List<Entry> add(List<Entry> existing, String path, String packageName) {
        List<Entry> updated = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return new ArrayList(existing);
        }
        String pkg = packageName == null ? "" : packageName;
        if (pkg.isEmpty()) {
            Iterator<Entry> it = existing.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Entry e = it.next();
                if (path.equals(e.path) && !e.packageName.isEmpty()) {
                    pkg = e.packageName;
                    break;
                }
            }
        }
        updated.add(new Entry(path, pkg));
        for (Entry e2 : existing) {
            if (!path.equals(e2.path) && (pkg.isEmpty() || !pkg.equals(e2.packageName))) {
                if (updated.size() >= 5) {
                    break;
                }
                updated.add(e2);
            }
        }
        return updated;
    }

    public static List<Entry> remove(List<Entry> existing, Entry target) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : existing) {
            if (!e.path.equals(target.path) && (target.packageName.isEmpty() || !target.packageName.equals(e.packageName))) {
                out.add(e);
            }
        }
        return out;
    }
}
