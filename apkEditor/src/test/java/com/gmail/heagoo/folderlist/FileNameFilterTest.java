package com.gmail.heagoo.folderlist;
import org.junit.Test;
import static org.junit.Assert.*;
public class FileNameFilterTest {
    @Test public void matchesCaseInsensitiveAndKeepsParent() {
        assertTrue(FileNameFilter.matches("Test.APK", "t.a"));
        assertTrue(FileNameFilter.matches("..", "nothing"));
        assertTrue(FileNameFilter.matches("file", ""));
        assertFalse(FileNameFilter.matches("file", "other"));
    }
}
