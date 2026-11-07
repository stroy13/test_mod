package com.gmail.heagoo.appdm.util;

import com.gmail.heagoo.common.NaturalOrderComparator;
import java.util.Comparator;

/** Natural name order for the data/file browser; folders stay before files. */
public class FilenameComparator implements Comparator<FileRecord> {
    private final NaturalOrderComparator names = new NaturalOrderComparator();

    @Override
    public int compare(FileRecord a, FileRecord b) {
        boolean upA = a.isDir && "..".equals(a.fileName);
        boolean upB = b.isDir && "..".equals(b.fileName);
        if (upA != upB) return upA ? -1 : 1;
        if (a.isDir != b.isDir) return a.isDir ? -1 : 1;
        return names.compare(a.fileName, b.fileName);
    }
}
