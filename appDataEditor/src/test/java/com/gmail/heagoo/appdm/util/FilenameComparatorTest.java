package com.gmail.heagoo.appdm.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FilenameComparatorTest {
    private static FileRecord record(String name, boolean isDir) {
        FileRecord record = new FileRecord();
        record.fileName = name;
        record.isDir = isDir;
        return record;
    }

    @Test
    public void parentFoldersAndNaturalFileNames() {
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                record("file10", false), record("..", true),
                record("file2", false), record("dir", true)));
        records.sort(new FilenameComparator());
        assertEquals(Arrays.asList("..", "dir", "file2", "file10"), Arrays.asList(
                records.get(0).fileName, records.get(1).fileName,
                records.get(2).fileName, records.get(3).fileName));
    }
}
