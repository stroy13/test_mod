package com.gmail.heagoo.apkeditor.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ModifiedTreeTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void strictAndInclusiveMatchOriginalTimestampRules() throws Exception {
        File root = folder.newFolder("root");
        File sub = new File(root, "nested"); assertTrue(sub.mkdir());
        File file = new File(sub, "test.smali"); Files.write(file.toPath(), new byte[]{1});
        assertTrue(root.setLastModified(100000));
        assertTrue(sub.setLastModified(100000));
        assertTrue(file.setLastModified(200000));
        assertFalse(ModifiedTree.newerThan(root, 200000));
        assertTrue(ModifiedTree.atLeast(root, 200000));
        assertTrue(ModifiedTree.newerThan(root, 199999));
        assertFalse(ModifiedTree.atLeast(root, 200001));
        assertTrue(file.delete());
        // A deletion changes the parent directory timestamp and must be detected.
        assertTrue(ModifiedTree.newerThan(root, 200000));
    }
    @Test public void returnsEarlyWithoutWalkingRemainingSubtrees() throws Exception {
        FakeFile slow = new FakeFile("slow", 1, true, null);
        FakeFile changed = new FakeFile("changed", 100, false, null);
        FakeFile root = new FakeFile("root", 1, true, new File[]{slow, changed});
        assertEquals(changed, ModifiedTree.firstNewer(root, 10));
        assertFalse("unneeded directory should not be listed", slow.listed);
    }
    @Test public void unreadableDirectoryDoesNotSilentlySkipChanges() throws Exception {
        try {
            ModifiedTree.newerThan(new FakeFile("unreadable", 1, true, null), 10);
            fail("expected failure");
        } catch (IOException expected) { assertTrue(expected.getMessage().contains("Cannot scan")); }
    }
    private static final class FakeFile extends File {
        final long modified; final boolean directory; final File[] children; boolean listed;
        FakeFile(String name, long modified, boolean directory, File[] children) {
            super(name); this.modified = modified; this.directory = directory; this.children = children;
        }
        @Override public long lastModified() { return modified; }
        @Override public boolean isDirectory() { return directory; }
        @Override public File[] listFiles() { listed = true; return children; }
    }
}
