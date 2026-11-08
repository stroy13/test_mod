package com.gmail.heagoo.apkeditor.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApkStructureValidatorTest {

    private static byte[] stringPool(String[] strings, boolean utf8) {
        int count = strings.length;
        int headerSize = 28 + count * 4;
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        int[] offsets = new int[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = data.size();
            byte[] encoded;
            if (utf8) {
                byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_8);
                encoded = new byte[2 + bytes.length + 1];
                encoded[0] = (byte) strings[i].length();
                encoded[1] = (byte) bytes.length;
                System.arraycopy(bytes, 0, encoded, 2, bytes.length);
            } else {
                byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_16LE);
                encoded = new byte[2 + bytes.length + 2];
                ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).putShort((short) strings[i].length());
                System.arraycopy(bytes, 0, encoded, 2, bytes.length);
            }
            data.write(encoded, 0, encoded.length);
        }
        byte[] strData = data.toByteArray();
        int padding = (4 - ((headerSize + strData.length) % 4)) % 4;
        int chunkSize = headerSize + strData.length + padding;
        ByteBuffer buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0x0001);
        buf.putShort((short) 28);
        buf.putInt(chunkSize);
        buf.putInt(count);
        buf.putInt(0);
        buf.putInt(utf8 ? 0x100 : 0);
        buf.putInt(headerSize);
        buf.putInt(0);
        for (int offset : offsets) buf.putInt(offset);
        buf.put(strData);
        return buf.array();
    }

    private static byte[] arsc(String[] strings, boolean utf8) {
        byte[] pool = stringPool(strings, utf8);
        byte[] pkg = new byte[288];
        ByteBuffer k = ByteBuffer.wrap(pkg).order(ByteOrder.LITTLE_ENDIAN);
        k.putShort((short) 0x0200);
        k.putShort((short) 288);
        k.putInt(288);
        k.putInt(0x7f);
        k.position(16);
        k.put("com.test.app".getBytes(StandardCharsets.UTF_16LE));
        int size = 12 + pool.length + pkg.length;
        byte[] table = new byte[size];
        ByteBuffer t = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN);
        t.putShort((short) 0x0002);
        t.putShort((short) 12);
        t.putInt(size);
        t.putInt(1);
        t.put(pool);
        t.put(pkg);
        return table;
    }

    @Test
    public void acceptsValidUtf8AndUtf16ResourceTables() throws Exception {
        ApkStructureValidator.validateResources(arsc(new String[]{"label", "com.test.app"}, true));
        ApkStructureValidator.validateResources(arsc(new String[]{"label", "com.test.app"}, false));
    }

    @Test
    public void rejectsTruncatedTable() {
        byte[] table = arsc(new String[]{"label"}, true);
        byte[] truncated = new byte[table.length - 4];
        System.arraycopy(table, 0, truncated, 0, truncated.length);
        try {
            ApkStructureValidator.validateResources(truncated);
            fail("Expected truncation error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("size mismatch"));
        }
    }

    @Test
    public void rejectsBadStringOffset() throws Exception {
        byte[] table = arsc(new String[]{"label"}, true);
        ByteBuffer b = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN);
        int poolPos = 12;
        int stringsStart = b.getInt(poolPos + 20);
        b.putInt(poolPos + 28, stringsStart + 10000);
        try {
            ApkStructureValidator.validateResources(table);
            fail("Expected offset error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("offset out of bounds"));
        }
    }

    @Test
    public void rejectsNonBinaryManifest() {
        try {
            ApkStructureValidator.validateManifest("<manifest/>".getBytes(StandardCharsets.UTF_8));
            fail("Expected AXML error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("not binary AXML"));
        }
    }
}
