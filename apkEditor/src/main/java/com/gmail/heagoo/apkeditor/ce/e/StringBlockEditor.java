package com.gmail.heagoo.apkeditor.ce.e;

import com.gmail.heagoo.apkeditor.ce.e.ResourceEditor.ResChunkHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import brut.androlib.res.decoder.StringBlock;

/**
 * Edits the global string pool of resources.arsc.
 *
 * <p>The old implementation shifted offsets after every single replacement and added
 * padding repeatedly, which corrupted pools with shared string offsets or multiple
 * edits. This version decodes the pool once, applies all replacements, then rebuilds
 * the pool with correct UTF-8/UTF-16 lengths, shared offsets and a single alignment
 * padding before the style data.
 */
public class StringBlockEditor {

    private final ResChunkHeader strPoolHeader;
    private byte[] strPoolData;
    private final int originalChunkSize;
    private final int stringCount;
    private final int styleCount;
    private final boolean isUTF8;
    private final int[] stringOffsets;
    private final int stringsOffset;
    private final int stylesOffset;
    private final byte[] styleData;
    private final String[] values;
    private int totalDeltaSize;

    public StringBlockEditor(ResChunkHeader strPoolHeader, byte[] strPoolData) throws IOException {
        this.strPoolHeader = strPoolHeader;
        this.strPoolData = strPoolData;
        this.originalChunkSize = strPoolHeader.chunkSize;
        if (strPoolData.length < 20) {
            throw new IOException("Truncated resources.arsc string pool");
        }
        this.stringCount = getInt(strPoolData, 0);
        this.styleCount = getInt(strPoolData, 4);
        int flags = getInt(strPoolData, 8);
        this.stringsOffset = getInt(strPoolData, 12);
        this.stylesOffset = getInt(strPoolData, 16);
        this.isUTF8 = (flags & 0x100) != 0;
        if (stringCount < 0 || styleCount < 0 || stringsOffset < 28
                || stringsOffset > strPoolData.length + 8
                || (styleCount > 0 && (stylesOffset < stringsOffset || stylesOffset > strPoolData.length + 8))) {
            throw new IOException("Invalid resources.arsc string pool offsets");
        }
        this.stringOffsets = new int[stringCount];
        int offset = 20;
        for (int i = 0; i < stringCount; i++) {
            if (offset + 4 > strPoolData.length) {
                throw new IOException("Truncated resources.arsc string offsets");
            }
            stringOffsets[i] = getInt(strPoolData, offset);
            offset += 4;
        }
        this.styleData = stylesOffset > 0
                ? Arrays.copyOfRange(strPoolData, stylesOffset - 8, strPoolData.length)
                : new byte[0];
        this.values = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            values[i] = decodeString(i);
            if (values[i] == null) {
                throw new IOException("Cannot decode resources.arsc string #" + i);
            }
        }
    }

    private String decodeString(int index) {
        try {
            int start = stringsOffset + stringOffsets[index] - 8;
            if (start < 0 || start >= strPoolData.length) {
                return null;
            }
            int offset;
            int length;
            if (isUTF8) {
                int[] val = StringBlock.getUtf8(strPoolData, start);
                offset = val[0];
                length = val[1];
            } else {
                int[] val = StringBlock.getUtf16(strPoolData, start);
                offset = start + val[0];
                length = val[1];
            }
            if (offset < 0 || length < 0 || offset + length > strPoolData.length) {
                return null;
            }
            return (isUTF8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16LE)
                    .decode(ByteBuffer.wrap(strPoolData, offset, length)).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean replaceString(String oldStr, String newStr,
                                 String oldStarts, String newStarts) throws IOException {
        boolean replaced = false;
        for (int i = 0; i < stringCount; i++) {
            String value = values[i];
            String newValue = null;
            if (oldStr != null && newStr != null && value.equals(oldStr)) {
                newValue = newStr;
            } else if (oldStarts != null && newStarts != null && value.startsWith(oldStarts)) {
                newValue = newStarts + value.substring(oldStarts.length());
            }
            if (newValue != null && !newValue.equals(value)) {
                values[i] = newValue;
                replaced = true;
            }
        }
        if (replaced) {
            rebuild();
        }
        return replaced;
    }

    private void rebuild() throws IOException {
        byte[][] encoded = new byte[stringCount][];
        int dataSize = 0;
        for (int i = 0; i < stringCount; i++) {
            encoded[i] = isUTF8 ? encodeUtf8(values[i]) : encodeUtf16(values[i]);
            dataSize += encoded[i].length;
        }
        int headerSize = 28 + stringCount * 4 + styleCount * 4;
        int padding = (4 - ((headerSize + dataSize) % 4)) % 4;
        int newStylesOffset = styleCount > 0 ? headerSize + dataSize + padding : 0;
        int newChunkSize = headerSize + dataSize + padding + styleData.length;
        byte[] newData = new byte[newChunkSize - 8];
        putInt(newData, 0, stringCount);
        putInt(newData, 4, styleCount);
        putInt(newData, 8, isUTF8 ? 0x100 : 0);
        putInt(newData, 12, headerSize);
        putInt(newData, 16, newStylesOffset);
        Map<ByteBuffer, Integer> sharedOffsets = new HashMap<>();
        int cursor = headerSize - 8;
        int offsetArrayPos = 20;
        for (int i = 0; i < stringCount; i++) {
            ByteBuffer key = ByteBuffer.wrap(encoded[i]);
            Integer shared = sharedOffsets.get(key);
            int stringOffset;
            if (shared != null) {
                stringOffset = shared;
            } else {
                stringOffset = cursor - (headerSize - 8);
                sharedOffsets.put(key, stringOffset);
                System.arraycopy(encoded[i], 0, newData, cursor, encoded[i].length);
                cursor += encoded[i].length;
            }
            putInt(newData, offsetArrayPos, stringOffset);
            offsetArrayPos += 4;
        }
        // Style offsets array is preserved verbatim between string offsets and string data.
        int styleArraySrc = 20 + stringCount * 4;
        int styleArrayDst = styleArraySrc;
        if (styleCount > 0) {
            System.arraycopy(strPoolData, styleArraySrc, newData, styleArrayDst, styleCount * 4);
            System.arraycopy(styleData, 0, newData, newStylesOffset - 8, styleData.length);
        }
        strPoolHeader.setChunkSize(newChunkSize);
        strPoolData = newData;
        totalDeltaSize = newChunkSize - originalChunkSize;
    }

    private static byte[] encodeUtf8(String value) throws IOException {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        int utf16Length = value.codePointCount(0, value.length());
        if (utf16Length > 0x7fff || utf8.length > 0x7fff) {
            throw new IOException("resources.arsc string is too long: " + utf16Length);
        }
        byte[] len1 = lengthBytes(utf16Length);
        byte[] len2 = lengthBytes(utf8.length);
        byte[] result = new byte[len1.length + len2.length + utf8.length + 1];
        System.arraycopy(len1, 0, result, 0, len1.length);
        System.arraycopy(len2, 0, result, len1.length, len2.length);
        System.arraycopy(utf8, 0, result, len1.length + len2.length, utf8.length);
        return result;
    }

    private static byte[] lengthBytes(int value) {
        if (value < 0x80) {
            return new byte[]{(byte) value};
        }
        return new byte[]{(byte) ((value >> 8) | 0x80), (byte) (value & 0xff)};
    }

    private static byte[] encodeUtf16(String value) throws IOException {
        byte[] utf16 = value.getBytes(StandardCharsets.UTF_16LE);
        int characters = utf16.length / 2;
        if (characters > 0x7fff) {
            throw new IOException("resources.arsc UTF-16 string is too long: " + characters);
        }
        byte[] result = new byte[2 + utf16.length + 2];
        result[0] = (byte) (characters & 0xff);
        result[1] = (byte) ((characters >> 8) & 0xff);
        System.arraycopy(utf16, 0, result, 2, utf16.length);
        return result;
    }

    public int getDeltaSize() {
        return totalDeltaSize;
    }

    public void writeTo(OutputStream output) throws IOException {
        strPoolHeader.writeTo(output);
        output.write(strPoolData);
    }

    private static int getInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff) | ((buffer[offset + 1] & 0xff) << 8)
                | ((buffer[offset + 2] & 0xff) << 16) | ((buffer[offset + 3] & 0xff) << 24);
    }

    private static void putInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) value;
        buffer[offset + 1] = (byte) (value >> 8);
        buffer[offset + 2] = (byte) (value >> 16);
        buffer[offset + 3] = (byte) (value >> 24);
    }
}
