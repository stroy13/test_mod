package com.gmail.heagoo.folderlist;

import com.gmail.heagoo.common.NaturalOrderComparator;
import java.util.Comparator;
import java.util.Locale;

/** Sorting used by the file browser and every file-selection dialog. */
public final class FileSort implements Comparator<FileRecord> {
    public static final int NAME = 0;
    public static final int MODIFIED = 1;
    public static final int SIZE = 2;
    public static final int TYPE = 3;
    private final boolean descending;
    private final int key;
    private final NaturalOrderComparator names;

    public FileSort(int key, boolean descending) {
        this(key, descending, Locale.getDefault());
    }

    public FileSort(int key, boolean descending, Locale locale) {
        this.key = normalizeKey(key);
        this.descending = descending;
        names = new NaturalOrderComparator(locale);
    }

    public static int normalizeKey(int key) {
        return key < NAME || key > TYPE ? NAME : key;
    }

    public int getKey() {
        return key;
    }

    public boolean isDescending() {
        return descending;
    }

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1);
    }

    @Override
    public int compare(FileRecord a, FileRecord b) {
        boolean upA = a.isDir && "..".equals(a.fileName);
        boolean upB = b.isDir && "..".equals(b.fileName);
        if (upA != upB) return upA ? -1 : 1;
        // Keep navigation and folder grouping predictable in both directions.
        if (a.isDir != b.isDir) return a.isDir ? -1 : 1;
        int result = 0;
        if (key == MODIFIED) {
            result = Long.compare(a.lastModified, b.lastModified);
        } else if (key == SIZE && !a.isDir) {
            result = Long.compare(a.totalSize, b.totalSize);
        } else if (key == TYPE && !a.isDir) {
            result = names.compare(extension(a.fileName), extension(b.fileName));
        }
        if (result == 0) result = names.compare(a.fileName, b.fileName);
        return descending ? -Integer.signum(result) : Integer.signum(result);
    }
}
