package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalModelStoreTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void importsWithoutOverwriting() throws Exception {
        byte[] data = new byte[]{'G','G','U','F',3,0,0,0,1,2,3};
        java.io.File a = LocalModelStore.importModel(new ByteArrayInputStream(data), folder.getRoot());
        java.io.File b = LocalModelStore.importModel(new ByteArrayInputStream(data), folder.getRoot());
        assertNotEquals(a, b);
        assertArrayEquals(data, Files.readAllBytes(a.toPath()));
        assertArrayEquals(data, Files.readAllBytes(b.toPath()));
    }
    @Test public void invalidAndTruncatedHeadersLeaveNoFiles() throws Exception {
        for (byte[] data : new byte[][]{{'G','G','U','F'}, {'G','G','U','F',1,0,0,0}, {'N','O','P','E',3,0,0,0}}) {
            try { LocalModelStore.importModel(new ByteArrayInputStream(data), folder.getRoot()); fail(); }
            catch (IOException expected) { assertEquals(0, folder.getRoot().list().length); }
        }
    }
    @Test public void readFailureCleansPartialFile() throws Exception {
        byte[] header = {'G','G','U','F',3,0,0,0};
        InputStream broken = new InputStream() {
            int pos;
            @Override public int read() throws IOException {
                if (pos == header.length) throw new IOException("broken");
                return header[pos++];
            }
        };
        try { LocalModelStore.importModel(broken, folder.getRoot()); fail(); }
        catch (IOException expected) { assertEquals(0, folder.getRoot().list().length); }
    }
}
