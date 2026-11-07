package com.gmail.heagoo.folderlist;

import java.util.Comparator;
import java.util.Locale;

/* loaded from: /tmp/apkx/classes7.dex */
public final class FileSort implements Comparator<FileRecord> {
    public static final int MODIFIED = 1;
    public static final int NAME = 0;
    public static final int SIZE = 2;
    public static final int TYPE = 3;
    private final boolean descending;
    private final int key;

    public FileSort(int key, boolean descending) {
        this.key = (key < 0 || key > 3) ? 0 : key;
        this.descending = descending;
    }

    private static int number(long a, long b) {
        if (a < b) {
            return -1;
        }
        return a > b ? 1 : 0;
    }

    private static int name(String a, String b) {
        int r = a.compareToIgnoreCase(b);
        return r != 0 ? r : a.compareTo(b);
    }

    private static String extension(String n) {
        int i = n.lastIndexOf(46);
        return (i <= 0 || i == n.length() + (-1)) ? "" : n.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    @Override // java.util.Comparator
    public int compare(FileRecord a, FileRecord b) {
        boolean upB = false;
        boolean upA = a.isDir && "..".equals(a.fileName);
        if (b.isDir && "..".equals(b.fileName)) {
            upB = true;
        }
        if (upA != upB) {
            return upA ? -1 : 1;
        }
        if (a.isDir != b.isDir) {
            return a.isDir ? -1 : 1;
        }
        int r = 0;
        if (this.key == 1) {
            r = number(a.lastModified, b.lastModified);
        } else if (this.key == 2 && !a.isDir) {
            r = number(a.totalSize, b.totalSize);
        } else if (this.key == 3 && !a.isDir) {
            r = extension(a.fileName).compareTo(extension(b.fileName));
        }
        if (r == 0) {
            r = name(a.fileName, b.fileName);
        }
        return this.descending ? -Integer.signum(r) : Integer.signum(r);
    }
}
