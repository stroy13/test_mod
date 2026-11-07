package com.gmail.heagoo.folderlist;

import java.io.File;
import java.util.List;

/** Keeps metadata and ordering consistent when a directory is opened or changed. */
public final class FileListSorter {
    private FileListSorter() {
    }

    public static void sort(List<FileRecord> records, int key, boolean descending,
                            String directory) {
        int normalized = FileSort.normalizeKey(key);
        if (directory != null && (normalized == FileSort.MODIFIED || normalized == FileSort.SIZE)) {
            for (FileRecord record : records) {
                if (record.isInZip || (record.isDir && "..".equals(record.fileName))) continue;
                File file = new File(directory, record.fileName);
                record.lastModified = file.lastModified();
                record.totalSize = record.isDir ? -1L : file.length();
            }
        }
        records.sort(new FileSort(normalized, descending));
    }
}
