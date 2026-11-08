package com.gmail.heagoo.apkeditor.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.Adler32;

/**
 * Lightweight structural checks for the binary files an APK installer must parse.
 * Signature verification alone does not prove that edited AXML/ARSC/DEX payloads are
 * well-formed; these checks catch truncated chunks, bad string-pool offsets and DEX
 * checksum mismatches before an unusable APK is published.
 */
public final class ApkStructureValidator {
    private ApkStructureValidator() {
    }

    public static void validateManifest(byte[] data) throws IOException {
        ByteBuffer b = buffer(data);
        if (data.length < 8) throw new IOException("AndroidManifest.xml is truncated");
        int type = b.getShort(0) & 0xffff;
        int headerSize = b.getShort(2) & 0xffff;
        int fileSize = b.getInt(4);
        if (type != 0x0003) throw new IOException("AndroidManifest.xml is not binary AXML (type=0x"
                + Integer.toHexString(type) + ")");
        if (headerSize < 8) throw new IOException("Invalid AXML header size: " + headerSize);
        if (fileSize != data.length) throw new IOException("AXML size mismatch: header=" + fileSize
                + ", actual=" + data.length);
        int pos = headerSize;
        boolean sawStringPool = false;
        boolean sawEnd = false;
        int stringCount = -1;
        while (pos < data.length) {
            if (pos + 8 > data.length) throw new IOException("Truncated AXML chunk at " + pos);
            int chunkType = b.getShort(pos) & 0xffff;
            int chunkHeader = b.getShort(pos + 2) & 0xffff;
            int chunkSize = b.getInt(pos + 4);
            if (chunkHeader < 8 || chunkSize < chunkHeader || pos + chunkSize > data.length) {
                throw new IOException("Invalid AXML chunk at " + pos + ": type=0x"
                        + Integer.toHexString(chunkType) + ", header=" + chunkHeader + ", size=" + chunkSize);
            }
            if (chunkType == 0x0001) {
                sawStringPool = true;
                validateStringPool(b, pos, chunkSize, chunkHeader, "AndroidManifest.xml");
                stringCount = b.getInt(pos + 8);
            } else if (chunkType == 0x0180) {
                if (chunkHeader < 8 || (chunkSize - chunkHeader) % 4 != 0) {
                    throw new IOException("Invalid AXML resource map chunk at " + pos);
                }
            } else if (chunkType == 0x0100 || chunkType == 0x0101) {
                if (chunkSize >= 24 && stringCount >= 0) {
                    checkStringIndex(b.getInt(pos + 16), stringCount, "namespace prefix", pos);
                    checkStringIndex(b.getInt(pos + 20), stringCount, "namespace uri", pos);
                }
                if (chunkType == 0x0101) sawEnd = true;
            } else if (chunkType == 0x0103) {
                if (chunkSize < 24) throw new IOException("Truncated AXML end tag at " + pos);
                if (stringCount >= 0) {
                    checkStringIndex(b.getInt(pos + 16), stringCount, "end tag namespace", pos);
                    checkStringIndex(b.getInt(pos + 20), stringCount, "end tag name", pos);
                }
            } else if (chunkType == 0x0104) {
                if (chunkSize >= 20 && stringCount >= 0) {
                    checkStringIndex(b.getInt(pos + 16), stringCount, "CDATA string", pos);
                }
            } else if (chunkType == 0x0102) {
                if (chunkSize < 36) throw new IOException("Truncated AXML start tag at " + pos);
                int attrCount = b.getShort(pos + 28) & 0xffff;
                int attrStart = b.getShort(pos + 26) & 0xffff;
                int attrSize = b.getShort(pos + 24) & 0xffff;
                if (attrSize != 0 && attrSize != 20) {
                    throw new IOException("Unexpected AXML attribute size: " + attrSize);
                }
                if (36 + attrCount * 20 > chunkSize) {
                    throw new IOException("AXML attribute table overruns chunk at " + pos);
                }
                if (attrStart != 0 && attrStart + attrCount * 20 > chunkSize) {
                    throw new IOException("AXML attribute start overruns chunk at " + pos);
                }
                if (stringCount >= 0) {
                    checkStringIndex(b.getInt(pos + 16), stringCount, "start tag namespace", pos);
                    checkStringIndex(b.getInt(pos + 20), stringCount, "start tag name", pos);
                    int attrsOffset = 16 + (attrStart != 0 ? attrStart : 20);
                    if (attrsOffset + attrCount * 20 > chunkSize) {
                        throw new IOException("AXML attributes overrun chunk at " + pos);
                    }
                    for (int i = 0; i < attrCount; i++) {
                        int off = pos + attrsOffset + i * 20;
                        if (off + 20 > pos + chunkSize) {
                            throw new IOException("Truncated AXML attribute at " + off);
                        }
                        checkStringIndex(b.getInt(off), stringCount, "attribute namespace", off);
                        checkStringIndex(b.getInt(off + 4), stringCount, "attribute name", off);
                        checkStringIndex(b.getInt(off + 8), stringCount, "attribute raw value", off);
                        int dataType = b.get(off + 15) & 0xff;
                        if (dataType == 0x03) {
                            checkStringIndex(b.getInt(off + 16), stringCount, "attribute string value", off);
                        }
                    }
                }
            }
            pos += chunkSize;
        }
        if (!sawStringPool) throw new IOException("AndroidManifest.xml has no string pool");
        if (!sawEnd) throw new IOException("AndroidManifest.xml has no end-of-document chunk");
    }

    public static void validateResources(byte[] data) throws IOException {
        ByteBuffer b = buffer(data);
        if (data.length < 12) throw new IOException("resources.arsc is truncated");
        int type = b.getShort(0) & 0xffff;
        int headerSize = b.getShort(2) & 0xffff;
        int tableSize = b.getInt(4);
        int packageCount = b.getInt(8);
        if (type != 0x0002) throw new IOException("resources.arsc is not a resource table");
        if (headerSize < 12) throw new IOException("Invalid resource table header size: " + headerSize);
        if (tableSize != data.length) throw new IOException("resources.arsc size mismatch: header="
                + tableSize + ", actual=" + data.length);
        if (packageCount < 1) throw new IOException("resources.arsc has no packages");
        int pos = headerSize;
        boolean sawStringPool = false;
        int packages = 0;
        while (pos < data.length) {
            if (pos + 8 > data.length) throw new IOException("Truncated ARSC chunk at " + pos);
            int chunkType = b.getShort(pos) & 0xffff;
            int chunkHeader = b.getShort(pos + 2) & 0xffff;
            int chunkSize = b.getInt(pos + 4);
            if (chunkHeader < 8 || chunkSize < chunkHeader || pos + chunkSize > data.length) {
                throw new IOException("Invalid ARSC chunk at " + pos + ": type=0x"
                        + Integer.toHexString(chunkType) + ", header=" + chunkHeader + ", size=" + chunkSize);
            }
            if (chunkType == 0x0001) {
                sawStringPool = true;
                validateStringPool(b, pos, chunkSize, chunkHeader, "resources.arsc");
            } else if (chunkType == 0x0200) {
                packages++;
                if (chunkHeader < 288 || chunkSize < chunkHeader) {
                    throw new IOException("Invalid ResTable_package header at " + pos);
                }
            }
            pos += chunkSize;
        }
        if (!sawStringPool) throw new IOException("resources.arsc has no global string pool");
        if (packages != packageCount) throw new IOException("resources.arsc package count mismatch: header="
                + packageCount + ", found=" + packages);
    }

    public static void validateDex(byte[] data) throws IOException {
        if (data.length < 112) throw new IOException("DEX is truncated");
        ByteBuffer b = buffer(data);
        if (!(data[0] == 'd' && data[1] == 'e' && data[2] == 'x' && data[3] == '\n')) {
            throw new IOException("Invalid DEX magic");
        }
        int fileSize = b.getInt(32);
        if (fileSize != data.length) throw new IOException("DEX size mismatch: header=" + fileSize
                + ", actual=" + data.length);
        Adler32 adler = new Adler32();
        adler.update(data, 12, data.length - 12);
        long expected = b.getInt(8) & 0xffffffffL;
        if (adler.getValue() != expected) throw new IOException("DEX checksum mismatch");
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(data, 32, data.length - 32);
            byte[] digest = sha1.digest();
            for (int i = 0; i < 20; i++) {
                if (digest[i] != data[12 + i]) throw new IOException("DEX SHA-1 signature mismatch");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot verify DEX signature", e);
        }
    }

    private static void checkStringIndex(int index, int stringCount, String what, int pos)
            throws IOException {
        if (index != -1 && (index < 0 || index >= stringCount)) {
            throw new IOException("AXML " + what + " index " + index
                    + " out of bounds (stringCount=" + stringCount + ") at " + pos);
        }
    }

    private static void validateStringPool(ByteBuffer b, int chunkPos, int chunkSize,
                                           int chunkHeader, String what) throws IOException {
        if (chunkHeader < 28 || chunkSize < chunkHeader) {
            throw new IOException(what + ": invalid string pool header");
        }
        int base = chunkPos;
        int stringCount = b.getInt(base + 8);
        int styleCount = b.getInt(base + 12);
        int flags = b.getInt(base + 16);
        int stringsStart = b.getInt(base + 20);
        int stylesStart = b.getInt(base + 24);
        boolean utf8 = (flags & 0x100) != 0;
        if (stringCount < 0 || styleCount < 0) throw new IOException(what + ": negative string counts");
        long offsetsEnd = 28L + 4L * stringCount + 4L * styleCount;
        if (offsetsEnd > chunkSize) throw new IOException(what + ": string offset table overruns chunk");
        if (stringsStart < offsetsEnd || stringsStart > chunkSize) {
            throw new IOException(what + ": invalid stringsStart=" + stringsStart);
        }
        if (styleCount > 0 && (stylesStart < stringsStart || stylesStart > chunkSize)) {
            throw new IOException(what + ": invalid stylesStart=" + stylesStart);
        }
        int dataEnd = styleCount > 0 ? stylesStart : chunkSize;
        byte[] raw = new byte[chunkSize];
        b.position(base);
        b.get(raw, 0, chunkSize);
        for (int i = 0; i < stringCount; i++) {
            int offset = b.getInt(base + 28 + i * 4);
            if (offset < 0 || stringsStart + offset >= dataEnd) {
                throw new IOException(what + ": string " + i + " offset out of bounds: " + offset);
            }
            int pos = stringsStart + offset;
            if (utf8) {
                int[] r = readLength8(raw, pos, dataEnd);
                pos = r[0];
                int byteLen = r[1];
                if (pos + byteLen >= dataEnd || raw[pos + byteLen] != 0) {
                    throw new IOException(what + ": unterminated UTF-8 string " + i);
                }
            } else {
                if (pos + 2 > dataEnd) throw new IOException(what + ": truncated UTF-16 length " + i);
                int units = readLength16(raw, pos);
                pos += units >= 0x8000 ? 4 : 2;
                int charLen = units >= 0x8000 ? ((units & 0x7fff) << 16) | (raw[pos - 2] & 0xff) | ((raw[pos - 1] & 0xff) << 8) : units;
                if (pos + charLen * 2 + 2 > dataEnd) {
                    throw new IOException(what + ": UTF-16 string " + i + " overruns pool");
                }
                if (raw[pos + charLen * 2] != 0 || raw[pos + charLen * 2 + 1] != 0) {
                    throw new IOException(what + ": unterminated UTF-16 string " + i);
                }
            }
        }
    }

    private static int[] readLength8(byte[] data, int pos, int end) throws IOException {
        if (pos >= end) throw new IOException("Truncated UTF-8 length");
        int val = data[pos] & 0xff;
        pos += (val & 0x80) != 0 ? 2 : 1;
        if (pos >= end) throw new IOException("Truncated UTF-8 length");
        val = data[pos] & 0xff;
        int length;
        if ((val & 0x80) != 0) {
            if (pos + 1 >= end) throw new IOException("Truncated UTF-8 length");
            length = ((val & 0x7f) << 8) | (data[pos + 1] & 0xff);
            pos += 2;
        } else {
            length = val;
            pos += 1;
        }
        return new int[]{pos, length};
    }

    private static int readLength16(byte[] data, int pos) {
        return (data[pos] & 0xff) | ((data[pos + 1] & 0xff) << 8);
    }

    private static ByteBuffer buffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }
}
