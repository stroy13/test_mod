package com.gmail.heagoo.folderlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class FileSortTest {
    private static FileRecord file(String name, long size, long modified) {
        FileRecord record = new FileRecord(name, false);
        record.totalSize = size;
        record.lastModified = modified;
        return record;
    }

    private static FileRecord dir(String name) {
        return new FileRecord(name, true);
    }

    private static List<String> names(List<FileRecord> records) {
        List<String> names = new ArrayList<>();
        for (FileRecord record : records) names.add(record.fileName);
        return names;
    }

    @Test
    public void parentAndFoldersStayFirstInBothDirections() {
        for (boolean descending : new boolean[]{false, true}) {
            List<FileRecord> records = new ArrayList<>(Arrays.asList(
                    file("b.txt", 1, 1), dir("z"), file("a.txt", 2, 2), dir(".."), dir("a")));
            records.sort(new FileSort(FileSort.NAME, descending, Locale.ROOT));
            assertEquals("..", records.get(0).fileName);
            assertTrue(records.get(1).isDir);
            assertTrue(records.get(2).isDir);
            assertTrue(!records.get(3).isDir);
            assertTrue(!records.get(4).isDir);
        }
    }

    @Test
    public void nameOrderIsNaturalAndReversible() {
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                file("file10", 0, 0), file("file2", 0, 0), file("file1", 0, 0)));
        records.sort(new FileSort(FileSort.NAME, false, Locale.ROOT));
        assertEquals(Arrays.asList("file1", "file2", "file10"), names(records));
        records.sort(new FileSort(FileSort.NAME, true, Locale.ROOT));
        assertEquals(Arrays.asList("file10", "file2", "file1"), names(records));
    }

    @Test
    public void sizeAndModifiedUseNumericValues() {
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                file("big", 30, 100), file("small", 10, 300), file("mid", 20, 200)));
        records.sort(new FileSort(FileSort.SIZE, false, Locale.ROOT));
        assertEquals(Arrays.asList("small", "mid", "big"), names(records));
        records.sort(new FileSort(FileSort.SIZE, true, Locale.ROOT));
        assertEquals(Arrays.asList("big", "mid", "small"), names(records));
        records.sort(new FileSort(FileSort.MODIFIED, false, Locale.ROOT));
        assertEquals(Arrays.asList("big", "mid", "small"), names(records));
        records.sort(new FileSort(FileSort.MODIFIED, true, Locale.ROOT));
        assertEquals(Arrays.asList("small", "mid", "big"), names(records));
    }

    @Test
    public void typeGroupsExtensionsAndInvalidKeysFallBackToName() {
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                file("b.txt", 0, 0), file("a.png", 0, 0), file("c.txt", 0, 0)));
        records.sort(new FileSort(FileSort.TYPE, false, Locale.ROOT));
        assertEquals(Arrays.asList("a.png", "b.txt", "c.txt"), names(records));
        assertEquals(FileSort.NAME, FileSort.normalizeKey(-1));
        assertEquals(FileSort.NAME, FileSort.normalizeKey(99));
        assertEquals(FileSort.SIZE, FileSort.normalizeKey(FileSort.SIZE));
    }

    @Test
    public void equalRecordsCompareAsZero() {
        FileSort sort = new FileSort(FileSort.MODIFIED, true, Locale.ROOT);
        assertEquals(0, sort.compare(file("a", 1, 2), file("a", 1, 2)));
    }
}
