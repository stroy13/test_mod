package com.gmail.heagoo.httpserver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileComparatorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void directoriesFirstThenNaturalFileNames() throws Exception {
        File dir = folder.newFolder("dir");
        File file10 = folder.newFile("file10");
        File file2 = folder.newFile("file2");
        List<File> files = new ArrayList<>(Arrays.asList(file10, file2, dir));
        files.sort(new FileComparator());
        assertEquals(Arrays.asList(dir, file2, file10), files);
    }
}
