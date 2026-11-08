package com.gmail.heagoo.apkeditor.ce.e;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the ResStringPool writer.
 *
 * <p>A renamed package used to be written as big-endian UTF-16 with a BOM because
 * String.getBytes("UTF-16") is platform dependent. Android read it back as
 * "\uFFFE\u7200\u7500..." and refused to install the APK. These tests pin the wire
 * format to little-endian, BOM-free, and round-trip every string through the writer
 * and the reader.
 */
public class ResStringChunkTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Builds a ResStringPool chunk exactly as aapt emits it. */
    private static byte[] pool(String[] strings, boolean utf8) {
        int count = strings.length;
        int stringsStart = 28 + count * 4;
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        int[] offsets = new int[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = data.size();
            byte[] encoded = utf8 ? utf8Entry(strings[i]) : utf16Entry(strings[i]);
            data.write(encoded, 0, encoded.length);
        }
        byte[] strData = data.toByteArray();
        int chunkSize = stringsStart + strData.length;
        int padding = (4 - (chunkSize % 4)) % 4;
        chunkSize += padding;
        ByteBuffer buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0x0001);
        buf.putShort((short) 28);
        buf.putInt(chunkSize);
        buf.putInt(count);
        buf.putInt(0);              // styleCount
        buf.putInt(utf8 ? 0x100 : 0);
        buf.putInt(stringsStart);
        buf.putInt(0);              // stylesStart
        for (int offset : offsets) {
            buf.putInt(offset);
        }
        buf.put(strData);
        return buf.array();
    }

    private static byte[] utf8Entry(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] len1 = length8(value.length());
        byte[] len2 = length8(bytes.length);
        byte[] out = new byte[len1.length + len2.length + bytes.length + 1];
        System.arraycopy(len1, 0, out, 0, len1.length);
        System.arraycopy(len2, 0, out, len1.length, len2.length);
        System.arraycopy(bytes, 0, out, len1.length + len2.length, bytes.length);
        return out;
    }

    private static byte[] length8(int value) {
        if (value < 0x80) {
            return new byte[]{(byte) value};
        }
        return new byte[]{(byte) ((value >> 8) | 0x80), (byte) (value & 0xff)};
    }

    private static byte[] utf16Entry(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_16LE);
        int units = bytes.length / 2;
        byte[] header;
        if (units > 32767) {
            header = new byte[4];
            ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort((short) (((units >> 16) & 0x7fff) | 0x8000))
                    .putShort((short) (units & 0xffff));
        } else {
            header = new byte[2];
            ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).putShort((short) units);
        }
        byte[] out = new byte[header.length + bytes.length + 2];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(bytes, 0, out, header.length, bytes.length);
        return out;
    }

    private static ResStringChunk parse(byte[] chunk) throws Exception {
        ResStringChunk parsed = new ResStringChunk();
        parsed.parse(new MyInputStream(new ByteArrayInputStream(chunk)));
        return parsed;
    }

    private byte[] dump(ResStringChunk chunk) throws Exception {
        File file = new File(folder.getRoot(), "pool-" + System.nanoTime() + ".bin");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(0);
        MyFileOutput out = new MyFileOutput(raf);
        try {
            chunk.dump(out);
        } finally {
            out.close();
        }
        return Files.readAllBytes(file.toPath());
    }

    private void roundTrip(String[] original, String[] replaced, boolean utf8) throws Exception {
        ResStringChunk chunk = parse(pool(original, utf8));
        assertEquals(original.length, chunk.getStringCount());
        for (int i = 0; i < original.length; i++) {
            assertEquals("parse " + utf8 + " #" + i, original[i], chunk.getStringByIndex(i));
        }
        for (int i = 0; i < replaced.length; i++) {
            if (replaced[i] != null) {
                chunk.modifyString(i, replaced[i]);
            }
        }
        ResStringChunk again = parse(dump(chunk));
        assertEquals(replaced.length, again.getStringCount());
        for (int i = 0; i < replaced.length; i++) {
            String expected = replaced[i] != null ? replaced[i] : original[i];
            assertEquals("round-trip utf8=" + utf8 + " #" + i, expected, again.getStringByIndex(i));
        }
    }

    @Test
    public void utf16RenamedPackageStaysLittleEndianWithoutBom() throws Exception {
        String[] original = {"ru.apkeditor.plus", "android", "manifest"};
        String[] replaced = {"ru.apkeditor.pluk", null, null};
        ResStringChunk chunk = parse(pool(original, false));
        chunk.modifyString(0, replaced[0]);
        byte[] written = dump(chunk);

        // The renamed package must appear as plain little-endian UTF-16 somewhere in
        // the pool, and the byte-swapped BOM must not appear at all.
        byte[] expected = replaced[0].getBytes(StandardCharsets.UTF_16LE);
        assertTrue("little-endian package bytes missing from pool", indexOf(written, expected) >= 0);
        byte[] swappedBom = {(byte) 0xfe, (byte) 0xff};
        assertEquals("pool must not contain a byte-swapped BOM", -1, indexOf(written, swappedBom));

        ResStringChunk again = parse(written);
        assertEquals("ru.apkeditor.pluk", again.getStringByIndex(0));
    }

    @Test
    public void utf16RoundTripsAsciiAndNonAscii() throws Exception {
        roundTrip(new String[]{"com.example.app", "Приложение", "日本語のラベル"},
                new String[]{"com.example.cloned", "Приложение 2", null}, false);
    }

    @Test
    public void utf8RoundTripsAsciiAndNonAscii() throws Exception {
        roundTrip(new String[]{"com.example.app", "Приложение", "日本語のラベル"},
                new String[]{"com.example.cloned", "Приложение 2", null}, true);
    }

    /**
     * Strings of 128 bytes or more use the two-byte big-endian length form. The old
     * writer emitted ULEB128 here, which the reader decoded to a bogus length.
     */
    @Test
    public void utf8RoundTripsLongStrings() throws Exception {
        StringBuilder longName = new StringBuilder("com.example.provider");
        while (longName.length() < 300) {
            longName.append(".authority_segment");
        }
        String value = longName.toString();
        roundTrip(new String[]{value, "short"},
                new String[]{value + ".cloned", null}, true);
    }

    @Test
    public void utf16RoundTripsLongStrings() throws Exception {
        StringBuilder longName = new StringBuilder("com.example.provider");
        while (longName.length() < 300) {
            longName.append(".authority_segment");
        }
        String value = longName.toString();
        roundTrip(new String[]{value, "short"},
                new String[]{value + ".cloned", null}, false);
    }

    /** Above 32767 code units the length spans two uint16 words with a 0x8000 flag. */
    @Test
    public void utf16RoundTripsVeryLongString() throws Exception {
        StringBuilder huge = new StringBuilder();
        while (huge.length() < 40000) {
            huge.append("abcdefghij");
        }
        String value = huge.toString();
        roundTrip(new String[]{value}, new String[]{value}, false);
    }

    @Test
    public void addedStringIsReadableAfterDump() throws Exception {
        ResStringChunk chunk = parse(pool(new String[]{"com.example.app", "android"}, false));
        chunk.addString("com.example.cloned.fileprovider", 1);
        ResStringChunk again = parse(dump(chunk));
        assertEquals(3, again.getStringCount());
        assertEquals("com.example.app", again.getStringByIndex(0));
        assertEquals("com.example.cloned.fileprovider", again.getStringByIndex(1));
        assertEquals("android", again.getStringByIndex(2));
    }

    @Test
    public void addedUtf8StringIsReadableAfterDump() throws Exception {
        ResStringChunk chunk = parse(pool(new String[]{"com.example.app", "android"}, true));
        chunk.addString("com.example.cloned.fileprovider", 1);
        ResStringChunk again = parse(dump(chunk));
        assertEquals(3, again.getStringCount());
        assertEquals("com.example.cloned.fileprovider", again.getStringByIndex(1));
    }

    /** Unmodified pools must survive a dump byte-for-byte. */
    @Test
    public void untouchedPoolIsPreserved() throws Exception {
        for (boolean utf8 : new boolean[]{false, true}) {
            byte[] original = pool(new String[]{"com.example.app", "android", "manifest"}, utf8);
            assertArrayEquals("utf8=" + utf8, original, dump(parse(original)));
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
