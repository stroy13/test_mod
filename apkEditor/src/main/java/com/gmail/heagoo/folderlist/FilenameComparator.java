package com.gmail.heagoo.folderlist;

import com.gmail.heagoo.common.NaturalOrderComparator;
import java.util.Comparator;
import java.util.Locale;

/** Parent entry first, then folders and files in natural name order. */
public class FilenameComparator implements Comparator<FileRecord> {
    private final NaturalOrderComparator names;

    public FilenameComparator() {
        this(Locale.getDefault());
    }

    public FilenameComparator(Locale locale) {
        names = new NaturalOrderComparator(locale);
    }

    @Override
    public int compare(FileRecord a, FileRecord b) {
        boolean upA = a.isDir && "..".equals(a.fileName);
        boolean upB = b.isDir && "..".equals(b.fileName);
        if (upA != upB) return upA ? -1 : 1;
        if (a.isDir != b.isDir) return a.isDir ? -1 : 1;
        return names.compare(a.fileName, b.fileName);
    }
}
