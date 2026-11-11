package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

/** Timestamp predicate which stops as soon as rebuilding is known to be needed. */
public final class ModifiedTree {
    private ModifiedTree() { }
    public static boolean newerThan(File root, long threshold) throws IOException {
        return firstNewer(root, threshold) != null;
    }
    public static boolean atLeast(File root, long threshold) throws IOException {
        return firstMatch(root, threshold, true) != null;
    }
    public static File firstNewer(File root, long threshold) throws IOException {
        return firstMatch(root, threshold, false);
    }
    private static File firstMatch(File root, long threshold, boolean inclusive) throws IOException {
        ArrayDeque<File> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            File file = pending.pop();
            long stamp = file.lastModified();
            if (inclusive ? stamp >= threshold : stamp > threshold) return file;
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null) throw new IOException("Cannot scan directory: " + file);
                for (File child : children) pending.push(child);
            }
        }
        return null;
    }
}
