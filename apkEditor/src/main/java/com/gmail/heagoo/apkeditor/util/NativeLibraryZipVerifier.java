package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/** Verify actual local ZIP payload offsets after signing, not merely extra-field lengths. */
final class NativeLibraryZipVerifier {
    private static final long U32_MAX = 0xffffffffL;
    private NativeLibraryZipVerifier() {}

    static boolean isNativeLibrary(String name) {
        if (!name.startsWith("lib/") || !name.endsWith(".so")) return false;
        int abiEnd = name.indexOf('/', 4);
        return abiEnd > 4 && abiEnd < name.length() - 4 && name.indexOf('/', abiEnd + 1) < 0;
    }

    static void verify(File file, boolean direct) throws IOException {
        if (!direct) return;
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            long length = in.length();
            int tailSize = (int) Math.min(length, 65557L);
            byte[] tail = new byte[tailSize];
            in.seek(length - tailSize);
            in.readFully(tail);
            int end = -1;
            for (int i = tail.length - 22; i >= 0; i--) {
                if (u32(tail, i) == 0x06054b50L && i + 22 + u16(tail, i + 20) == tail.length) { end = i; break; }
            }
            if (end < 0) throw new IOException("ZIP end record missing");
            long count = u16(tail, end + 10), centralSize = u32(tail, end + 12), centralOffset = u32(tail, end + 16);
            if (count == 65535 || centralSize == U32_MAX || centralOffset == U32_MAX)
                throw new IOException("ZIP64 native library verification is not supported");
            if (u16(tail, end + 4) != 0 || u16(tail, end + 6) != 0 || u16(tail, end + 8) != count
                    || centralOffset + centralSize > length - tailSize + end)
                throw new IOException("Invalid ZIP central directory");
            long position = centralOffset;
            for (long i = 0; i < count; i++) {
                if (position + 46 > centralOffset + centralSize) throw new IOException("Truncated ZIP central directory");
                byte[] h = new byte[46]; in.seek(position); in.readFully(h);
                if (u32(h, 0) != 0x02014b50L) throw new IOException("Invalid ZIP central entry");
                int method = u16(h, 10), nameLength = u16(h, 28), extraLength = u16(h, 30), commentLength = u16(h, 32);
                long localOffset = u32(h, 42);
                long next = position + 46L + nameLength + extraLength + commentLength;
                if (next > centralOffset + centralSize) throw new IOException("Truncated ZIP entry");
                byte[] nameBytes = new byte[nameLength]; in.readFully(nameBytes);
                String name = new String(nameBytes, StandardCharsets.UTF_8);
                if (isNativeLibrary(name)) {
                    if (method != 0) throw new IOException("Native library is compressed: " + name);
                    if (localOffset == U32_MAX || localOffset + 30 > length) throw new IOException("Invalid native library offset: " + name);
                    byte[] local = new byte[30]; in.seek(localOffset); in.readFully(local);
                    if (u32(local, 0) != 0x04034b50L || u16(local, 8) != 0) throw new IOException("Invalid native library header: " + name);
                    int localNameLength = u16(local, 26), localExtraLength = u16(local, 28);
                    if (localNameLength != nameBytes.length) throw new IOException("Native library name mismatch: " + name);
                    byte[] localName = new byte[localNameLength]; in.readFully(localName);
                    if (!java.util.Arrays.equals(nameBytes, localName)) throw new IOException("Native library name mismatch: " + name);
                    long data = localOffset + 30L + localNameLength + localExtraLength;
                    if (data % 16384 != 0) throw new IOException("Native library is not 16 KiB aligned: " + name + " offset=" + data);
                }
                position = next;
            }
        }
    }
    private static int u16(byte[] b, int p) { return (b[p] & 255) | ((b[p + 1] & 255) << 8); }
    private static long u32(byte[] b, int p) { return ((long) u16(b, p)) | (((long) u16(b, p + 2)) << 16); }
}
