package com.gmail.heagoo.apkeditor.ce.e;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** Strict little-endian reader for binary Android resources. */
public class MyInputStream {
    private int curOffset;
    private final InputStream is;

    public MyInputStream(InputStream is) {
        this.is = is;
    }

    public void readFully(byte[] buf) throws IOException {
        readFully(buf, 0, buf.length);
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > buf.length) {
            throw new IndexOutOfBoundsException("readFully(" + off + ", " + len + ")");
        }
        int read = 0;
        while (read < len) {
            int ret = is.read(buf, off + read, len - read);
            if (ret < 0) {
                throw new EOFException("Expected " + len + " bytes at offset " + curOffset
                        + ", got " + read);
            }
            if (ret > 0) {
                curOffset += ret;
                read += ret;
            }
        }
    }

    public int readBytes(byte[] buf, int off, int len) throws IOException {
        int ret = is.read(buf, off, len);
        if (ret > 0) {
            curOffset += ret;
        }
        return ret;
    }

    public int readShort() throws IOException {
        byte[] arr = new byte[2];
        readFully(arr);
        return (arr[0] & 0xff) | ((arr[1] & 0xff) << 8);
    }

    public int readInt() throws IOException {
        byte[] arr = new byte[4];
        readFully(arr);
        return ManifestEditorNew.getInt(arr, 0);
    }

    public void readIntArray(int[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = readInt();
        }
    }

    public void skip(int n) throws IOException {
        if (n < 0) {
            throw new IOException("Negative skip: " + n);
        }
        int remaining = n;
        byte[] buf = new byte[Math.min(8192, remaining)];
        while (remaining > 0) {
            int chunk = Math.min(buf.length, remaining);
            int skipped = (int) is.skip(chunk);
            if (skipped > 0) {
                remaining -= skipped;
                curOffset += skipped;
            } else {
                readFully(buf, 0, chunk);
                remaining -= chunk;
            }
        }
    }

    public int getPosition() {
        return curOffset;
    }

    public int readByte() throws IOException {
        int ret = is.read();
        if (ret < 0) {
            throw new EOFException("Unexpected end of resource at offset " + curOffset);
        }
        curOffset++;
        return ret;
    }
}
