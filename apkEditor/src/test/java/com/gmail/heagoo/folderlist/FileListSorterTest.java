package com.gmail.heagoo.folderlist;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileListSorterTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void sizeSortRefreshesMetadataFromDisk() throws Exception {
        File small = folder.newFile("small");
        File big = folder.newFile("big");
        try (FileOutputStream out = new FileOutputStream(big)) {
            out.write(new byte[32]);
        }
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                new FileRecord("big", false), new FileRecord("small", false)));
        FileListSorter.sort(records, FileSort.SIZE, false, folder.getRoot().getAbsolutePath());
        assertEquals("small", records.get(0).fileName);
        assertEquals(0L, records.get(0).totalSize);
        assertEquals(32L, records.get(1).totalSize);
        assertEquals(big.lastModified(), records.get(1).lastModified);
    }

    @Test
    public void zipRecordsAreNotStatLikeRealFiles() throws Exception {
        FileRecord zip = new FileRecord("missing", false, true, 123);
        zip.lastModified = 456;
        List<FileRecord> records = new ArrayList<>(Arrays.asList(zip));
        FileListSorter.sort(records, FileSort.SIZE, false, folder.getRoot().getAbsolutePath());
        assertEquals(123L, zip.totalSize);
        assertEquals(456L, zip.lastModified);
    }

    @Test
    public void modifiedSortRefreshesTimestamps() throws Exception {
        File old = folder.newFile("old");
        File recent = folder.newFile("recent");
        old.setLastModified(1000);
        recent.setLastModified(2000);
        List<FileRecord> records = new ArrayList<>(Arrays.asList(
                new FileRecord("recent", false), new FileRecord("old", false)));
        FileListSorter.sort(records, FileSort.MODIFIED, true, folder.getRoot().getAbsolutePath());
        assertEquals("recent", records.get(0).fileName);
        assertEquals("old", records.get(1).fileName);
    }
}
