package com.gmail.heagoo.apkeditor.ce.e;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringBlockEditorTest {

    private static byte[] pool(String[] strings, boolean utf8, int[] sharedOffsets) {
        int count = strings.length;
        int headerSize = 28 + count * 4;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offsets = new int[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = sharedOffsets != null ? sharedOffsets[i] : data.size();
            if (sharedOffsets != null && sharedOffsets[i] != data.size()) {
                continue;
            }
            byte[] encoded = encode(strings[i], utf8);
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

    private static byte[] encode(String value, boolean utf8) {
        if (utf8) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            ByteBuffer b = ByteBuffer.allocate(2 + bytes.length + 1);
            b.put((byte) value.length());
            b.put((byte) bytes.length);
            b.put(bytes);
            b.put((byte) 0);
            return b.array();
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_16LE);
        ByteBuffer b = ByteBuffer.allocate(2 + bytes.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) value.length());
        b.put(bytes);
        b.putShort((short) 0);
        return b.array();
    }

    private static ResourceEditor.ResChunkHeader header(byte[] pool) throws IOException {
        ResourceEditor.ResChunkHeader h = new ResourceEditor.ResChunkHeader();
        h.readFrom(new com.google.common.io.LittleEndianDataInputStream(
                new java.io.ByteArrayInputStream(pool)));
        return h;
    }

    private static String decode(byte[] pool, int index) {
        ByteBuffer b = ByteBuffer.wrap(pool).order(ByteOrder.LITTLE_ENDIAN);
        int stringsOffset = b.getInt(20);
        int offset = b.getInt(28 + index * 4);
        int start = stringsOffset + offset;
        int flags = b.getInt(16);
        if ((flags & 0x100) != 0) {
            int pos = start;
            if ((pool[pos] & 0x80) != 0) pos += 2; else pos += 1;
            int len = pool[pos] & 0xff;
            if ((len & 0x80) != 0) {
                len = ((len & 0x7f) << 8) | (pool[pos + 1] & 0xff);
                pos += 2;
            } else {
                pos += 1;
            }
            return new String(pool, pos, len, StandardCharsets.UTF_8);
        }
        int len = b.getShort(start) * 2;
        return new String(pool, start + 2, len, StandardCharsets.UTF_16LE);
    }

    @Test
    public void multipleUtf8ReplacementsKeepPoolValid() throws Exception {
        String[] strings = {"com.old.app", "com.old.app.Service", "com.old.app.Provider", "label"};
        byte[] original = pool(strings, true, null);
        ResourceEditor.ResChunkHeader h = header(original);
        byte[] data = new byte[original.length - 8];
        System.arraycopy(original, 8, data, 0, data.length);
        StringBlockEditor editor = new StringBlockEditor(h, data);
        assertTrue(editor.replaceString(null, null, "com.old.app", "com.new.application"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editor.writeTo(out);
        byte[] result = out.toByteArray();
        assertEquals(result.length, ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).getInt(4));
        assertEquals("com.new.application", decode(result, 0));
        assertEquals("com.new.application.Service", decode(result, 1));
        assertEquals("com.new.application.Provider", decode(result, 2));
        assertEquals("label", decode(result, 3));
        assertEquals(result.length - original.length, editor.getDeltaSize());
    }

    @Test
    public void sharedOffsetsArePreserved() throws Exception {
        String[] strings = {"same", "same", "other"};
        byte[] original = pool(strings, true, new int[]{0, 0, 7});
        ResourceEditor.ResChunkHeader h = header(original);
        byte[] data = new byte[original.length - 8];
        System.arraycopy(original, 8, data, 0, data.length);
        StringBlockEditor editor = new StringBlockEditor(h, data);
        assertTrue(editor.replaceString("same", "changed", null, null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editor.writeTo(out);
        byte[] result = out.toByteArray();
        ByteBuffer b = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(b.getInt(28), b.getInt(32));
        assertEquals("changed", decode(result, 0));
        assertEquals("changed", decode(result, 1));
        assertEquals("other", decode(result, 2));
    }

    @Test
    public void longUtf8StringIsNotTruncated() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append('x');
        String longValue = sb.toString();
        byte[] original = pool(new String[]{"old", "keep"}, true, null);
        ResourceEditor.ResChunkHeader h = header(original);
        byte[] data = new byte[original.length - 8];
        System.arraycopy(original, 8, data, 0, data.length);
        StringBlockEditor editor = new StringBlockEditor(h, data);
        assertTrue(editor.replaceString("old", longValue, null, null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editor.writeTo(out);
        assertEquals(longValue, decode(out.toByteArray(), 0));
    }
}
