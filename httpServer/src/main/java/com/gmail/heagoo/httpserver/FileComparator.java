package com.gmail.heagoo.httpserver;

import com.gmail.heagoo.common.NaturalOrderComparator;
import java.io.File;
import java.util.Comparator;

/** Same natural file order in the HTTP file browser as in the editor. */
public class FileComparator implements Comparator<File> {
    private final NaturalOrderComparator names = new NaturalOrderComparator();

    @Override
    public int compare(File a, File b) {
        boolean dirA = a.isDirectory();
        boolean dirB = b.isDirectory();
        if (dirA != dirB) return dirA ? -1 : 1;
        return names.compare(a.getName(), b.getName());
    }
}
