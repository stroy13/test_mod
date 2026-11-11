package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

/** Timestamp predicate which stops as soon as rebuilding is known to be needed. */
public final class ModifiedTree {
    private ModifiedTree() { }
    public static boolean newerThan(File root, long threshold) throws IOException {
        return matches(root, threshold, false);
    }
    public static boolean atLeast(File root, long threshold) throws IOException {
        return matches(root, threshold, true);
    }
    private static boolean matches(File root, long threshold, boolean inclusive) throws IOException {
        ArrayDeque<File> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            File file = pending.pop();
            long stamp = file.lastModified();
            if (inclusive ? stamp >= threshold : stamp > threshold) return true;
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null) throw new IOException("Cannot scan directory: " + file);
                for (File child : children) pending.push(child);
            }
        }
        return false;
    }
}
