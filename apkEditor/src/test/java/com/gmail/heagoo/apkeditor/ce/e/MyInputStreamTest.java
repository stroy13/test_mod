package com.gmail.heagoo.apkeditor.ce.e;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MyInputStreamTest {
    @Test
    public void readIntHandlesPartialReads() throws Exception {
        InputStream slow = new InputStream() {
            private final byte[] data = {0x78, 0x56, 0x34, 0x12};
            private int pos;
            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xff : -1;
            }
            @Override
            public int read(byte[] b, int off, int len) {
                if (pos >= data.length) return -1;
                b[off] = data[pos++];
                return 1;
            }
        };
        MyInputStream in = new MyInputStream(slow);
        assertEquals(0x12345678, in.readInt());
        assertEquals(4, in.getPosition());
    }

    @Test
    public void truncatedReadThrows() throws Exception {
        MyInputStream in = new MyInputStream(new ByteArrayInputStream(new byte[]{1, 2}));
        try {
            in.readInt();
            fail("Expected EOFException");
        } catch (EOFException expected) {
            // expected
        }
    }

    @Test
    public void skipFullyEvenWhenSkipReturnsZero() throws Exception {
        InputStream noSkip = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}) {
            @Override
            public long skip(long n) {
                return 0;
            }
        };
        MyInputStream in = new MyInputStream(noSkip);
        in.skip(3);
        assertEquals(4, in.readByte());
        assertEquals(4, in.getPosition());
    }
}
