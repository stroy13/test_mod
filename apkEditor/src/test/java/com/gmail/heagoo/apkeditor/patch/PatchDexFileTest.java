package com.gmail.heagoo.apkeditor.patch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Android 14+ rejects a writable file passed to DexClassLoader. The staging
 * helper must close the stream, remove the write bits, and never leave a
 * partial script behind after a copy failure.
 */
public class PatchDexFileTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void extractedScriptIsClosedAndReadOnly() throws Exception {
        byte[] bytes = "dex\n035\0pretend-script".getBytes(StandardCharsets.UTF_8);
        TrackingInput input = new TrackingInput(bytes);
        File dir = tmp.newFolder("private-dex");
        File dex = PatchDexFile.extract(dir, input);
        assertTrue("input must be closed before chmod", input.closed);
        assertTrue(dex.isFile());
        assertFalse("Android 14 rejects writable DEX", dex.canWrite());
        assertTrue(dex.canRead());
        assertArrayEquals(bytes, Files.readAllBytes(Paths.get(dex.getAbsolutePath())));
        assertEquals(dir.getAbsoluteFile(), dex.getParentFile().getAbsoluteFile());
    }

    @Test
    public void eachRunGetsItsOwnFileSoOptimizedOutputCannotGoStale() throws Exception {
        File dir = tmp.newFolder("private-dex");
        File first = PatchDexFile.extract(dir, new ByteArrayInputStream(new byte[]{1}));
        File second = PatchDexFile.extract(dir, new ByteArrayInputStream(new byte[]{2}));
        assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
        assertFalse(first.canWrite());
        assertFalse(second.canWrite());
    }

    @Test
    public void failedCopyDoesNotLeaveAPartialScript() throws Exception {
        File dir = tmp.newFolder("private-dex");
        InputStream broken = new InputStream() {
            private int served;

            @Override
            public int read() throws IOException {
                if (served++ == 0) {
                    return 'd';
                }
                throw new IOException("zip entry truncated");
            }
        };
        try {
            PatchDexFile.extract(dir, broken);
            fail("a broken zip entry must not produce a loadable script");
        } catch (IOException expected) {
        }
        File[] left = dir.listFiles();
        assertTrue("partial DEX must be removed", left == null || left.length == 0);
    }

    @Test
    public void missingPrivateDirectoryIsCreated() throws Exception {
        File dir = new File(tmp.getRoot(), "nested/private-dex");
        File dex = PatchDexFile.extract(dir, new ByteArrayInputStream(new byte[]{7}));
        assertTrue(dir.isDirectory());
        assertFalse(dex.canWrite());
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        boolean closed;

        TrackingInput(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            // ByteArrayInputStream has no native resource; record the close so
            // the test can prove staging closed the stream before chmod.
            closed = true;
        }
    }
}
