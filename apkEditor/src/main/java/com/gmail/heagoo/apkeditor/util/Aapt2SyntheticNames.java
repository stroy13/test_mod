package com.gmail.heagoo.apkeditor.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Adapt generated $ drawable names for AAPT2, then restore the table's key names. */
final class Aapt2SyntheticNames {
    private static final Pattern PUBLIC = Pattern.compile("<public\\b[^>]*\\btype\\s*=\\s*['\"]drawable['\"][^>]*>|<public\\b[^>]*\\bname\\s*=\\s*['\"]\\$[a-z0-9_]+['\"][^>]*>");
    private static final Pattern TYPE = Pattern.compile("\\btype\\s*=\\s*['\"]drawable['\"]");
    private static final Pattern NAME = Pattern.compile("\\bname\\s*=\\s*['\"](\\$[a-z0-9_]+)['\"]");
    private Aapt2SyntheticNames() {}

    static Map<String,String> find(Path res) throws IOException {
        Path publicXml = res.resolve("values/public.xml");
        if (!Files.isRegularFile(publicXml)) return Collections.emptyMap();
        String xml = new String(Files.readAllBytes(publicXml), StandardCharsets.UTF_8);
        if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("AAPT2 cannot safely process a public.xml with a DTD");
        if (xml.indexOf('$') < 0) return Collections.emptyMap();
        Matcher tags = PUBLIC.matcher(xml); Set<String> names = new TreeSet<>();
        while (tags.find()) {
            String tag = tags.group(); Matcher n = NAME.matcher(tag);
            if (TYPE.matcher(tag).find() && n.find()) names.add(n.group(1));
        }
        if (names.isEmpty()) return Collections.emptyMap();
        // Scan every source string once for all possible aliases. This preserves
        // the conservative substring collision rule without rescanning tens of
        // megabytes once for each of hundreds of names.
        Aapt2AliasScanner scanner = new Aapt2AliasScanner(names);
        try (java.util.stream.Stream<Path> paths = Files.walk(res)) {
            for (Path p : (Iterable<Path>) paths::iterator) if (Files.isRegularFile(p)) {
                scanner.scan(p.toString());
                if (p.getFileName().toString().endsWith(".xml")) {
                    if (Files.size(p) > 32L * 1024 * 1024) throw new IOException("XML too large for safe resource name adaptation: " + p);
                    scanner.scan(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                }
            }
        }
        Map<String,String> aliases = new LinkedHashMap<>();
        for (String name : names) {
            String alias = scanner.choose(name);
            if (alias == null) throw new IOException("No collision-free AAPT2 alias for " + name);
            aliases.put(name, alias);
        }
        return aliases;
    }

    static String rewriteXml(String xml, Map<String,String> aliases) throws IOException {
        if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("Cannot safely rewrite XML containing a DTD");
        // Most XML files have no synthetic names. Avoid tokenizing every element,
        // especially multi-megabyte values files on large applications.
        if (xml.indexOf('$') < 0 || aliases.isEmpty()) return xml;
        // Only resource references and name attributes, never ordinary text or CDATA/comments.
        Pattern tokens = Pattern.compile("<!--.*?-->|<!\\[CDATA\\[.*?]]>|<\\?.*?\\?>|<[^>]+>|[^<]+", Pattern.DOTALL);
        Pattern ref = Pattern.compile("([@?](?:[+*])?(?:[a-zA-Z0-9_.]+:)?drawable/)(\\$[a-z0-9_]+)(?![a-z0-9_])");
        Pattern attr = Pattern.compile("(\\bname\\s*=\\s*['\"])(\\$[a-z0-9_]+)(['\"])");
        Matcher parts = tokens.matcher(xml); StringBuffer out = new StringBuffer();
        while (parts.find()) {
            String part = parts.group();
            if (part.indexOf('$') >= 0 && !part.startsWith("<!--") && !part.startsWith("<![CDATA[") && !part.startsWith("<?")) {
                part = replace(part, ref, aliases);
                if (part.startsWith("<")) part = replace(part, attr, aliases);
            }
            parts.appendReplacement(out, Matcher.quoteReplacement(part));
        }
        parts.appendTail(out); return out.toString();
    }
    private static String replace(String s, Pattern pattern, Map<String,String> map) {
        Matcher m = pattern.matcher(s); StringBuffer b = new StringBuffer();
        while (m.find()) {
            String a = map.get(m.group(2));
            if (a != null) m.appendReplacement(b, Matcher.quoteReplacement(m.group(1) + a + (m.groupCount() == 3 ? m.group(3) : "")));
        }
        m.appendTail(b); return b.toString();
    }
    static void rewriteStage(Path stage, Map<String,String> aliases) throws IOException {
        List<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(stage)) { stream.filter(Files::isRegularFile).forEach(files::add); }
        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".xml")) {
                String s = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                String updated = rewriteXml(s, aliases);
                if (!s.equals(updated)) Files.write(file, updated.getBytes(StandardCharsets.UTF_8));
            }
            String dir = file.getParent().getFileName().toString();
            if (dir.equals("drawable") || dir.startsWith("drawable-")) {
                String f = file.getFileName().toString(); int dot = f.indexOf('.');
                if (dot > 0 && aliases.containsKey(f.substring(0, dot))) {
                    Files.move(file, file.resolveSibling(aliases.get(f.substring(0, dot)) + f.substring(dot)));
                }
            }
        }
    }

    static void restoreTable(byte[] bytes, Map<String,String> aliases) throws IOException {
        Map<String,String> reverse = new HashMap<>();
        for (Map.Entry<String,String> e : aliases.entrySet()) reverse.put(e.getValue(), e.getKey());
        Set<String> restored = new HashSet<>();
        if (u16(bytes, 0) != 2 || u32(bytes, 4) != bytes.length) throw new IOException("Invalid linked resource table");
        int p = u16(bytes, 2);
        while (p < bytes.length) {
            int type = u16(bytes, p), header = u16(bytes, p+2), size = checkedSize(bytes, p);
            if (type == 0x200) {
                if (header < 284) throw new IOException("Invalid resource package");
                long keyOffset = u32(bytes, p + 276);
                if (keyOffset < header || keyOffset >= size) throw new IOException("Invalid resource key pool");
                int pool = p + (int) keyOffset, poolSize = checkedSize(bytes, pool);
                if (pool + poolSize > p + size || u16(bytes, pool) != 1 || u16(bytes, pool+2) < 28) throw new IOException("Invalid resource key pool");
                long count = u32(bytes, pool + 8), flags = u32(bytes, pool + 16), start = u32(bytes, pool + 20);
                int ph = u16(bytes, pool+2);
                if (count > (poolSize - ph) / 4 || start >= poolSize) throw new IOException("Invalid key offsets");
                boolean changedPool = false;
                for (int i = 0; i < count; i++) {
                    long off = u32(bytes, pool + ph + i * 4);
                    if (off >= poolSize - start) throw new IOException("Invalid key offset");
                    int q = pool + (int) start + (int) off, end = pool + poolSize;
                    boolean utf8 = (flags & 0x100) != 0; int len;
                    if (utf8) { int[] x = readLength8(bytes, q, end); q = x[1]; x = readLength8(bytes, q, end); len = x[0]; q = x[1]; }
                    else { int[] x = readLength16(bytes, q, end); len = x[0] * 2; q = x[1]; }
                    if (len < 0 || q + (long)len + (utf8 ? 1 : 2) > end) throw new IOException("Invalid key string");
                    String s = new String(bytes, q, len, utf8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16LE);
                    String original = reverse.get(s);
                    if (original != null) { bytes[q] = '$'; restored.add(s); changedPool = true; }
                }
                // Renaming may invalidate the pool's lexical ordering. Android must
                // use a linear lookup rather than binary search in this pool.
                if (changedPool) bytes[pool + 16] &= (byte) ~1;
            }
            p += size;
        }
        if (p != bytes.length || restored.size() != aliases.size()) throw new IOException("AAPT2 resource aliases missing from linked table: " + restored.size() + "/" + aliases.size());
    }
    private static int checkedSize(byte[] b, int p) throws IOException { long n = u32(b,p+4); if (n < 8 || n > b.length-(long)p) throw new IOException("Invalid resource chunk size"); return (int)n; }
    private static int u16(byte[] b, int p) throws IOException { if (p < 0 || p+2L > b.length) throw new IOException("Truncated resource table"); return (b[p]&255)|((b[p+1]&255)<<8); }
    private static long u32(byte[] b, int p) throws IOException { return u16(b,p) | ((long)u16(b,p+2)<<16); }
    private static int[] readLength8(byte[] b,int p,int end) throws IOException { if (p>=end) throw new IOException("Truncated key"); int n=b[p++]&255; if ((n&128)!=0) { if (p>=end) throw new IOException("Truncated key"); n=((n&127)<<8)|(b[p++]&255); } return new int[]{n,p}; }
    private static int[] readLength16(byte[] b,int p,int end) throws IOException { if (p+2>end) throw new IOException("Truncated key"); int n=u16(b,p); p+=2; if ((n&0x8000)!=0) { if (p+2>end) throw new IOException("Truncated key"); n=((n&0x7fff)<<16)|u16(b,p); p+=2; } if (n>Integer.MAX_VALUE/2) throw new IOException("Key too long"); return new int[]{n,p}; }

    static void restoreApk(File apk, Map<String,String> aliases) throws IOException {
        File tmp = File.createTempFile("aapt2-restored-", ".apk", apk.getAbsoluteFile().getParentFile());
        try {
            boolean found = false;
            try (ZipFile zip = new ZipFile(apk); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp))) {
                Enumeration<? extends ZipEntry> entries = zip.entries(); byte[] buffer = new byte[65536];
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement(); ZipEntry n = new ZipEntry(e.getName());
                    if (e.getName().equals("resources.arsc")) {
                        if (found || e.getSize() < 0 || e.getSize() > 256L*1024*1024) throw new IOException("Invalid resource table in AAPT2 output");
                        found = true; byte[] data;
                        try (InputStream in = zip.getInputStream(e); ByteArrayOutputStream b = new ByteArrayOutputStream()) { int x; while ((x=in.read(buffer))!=-1) b.write(buffer,0,x); data=b.toByteArray(); }
                        restoreTable(data, aliases); CRC32 crc = new CRC32(); crc.update(data);
                        n.setMethod(ZipEntry.STORED); n.setSize(data.length); n.setCompressedSize(data.length); n.setCrc(crc.getValue());
                        out.putNextEntry(n); out.write(data); out.closeEntry();
                    } else {
                        if (e.getMethod() == ZipEntry.STORED) { n.setMethod(ZipEntry.STORED); n.setSize(e.getSize()); n.setCompressedSize(e.getSize()); n.setCrc(e.getCrc()); }
                        out.putNextEntry(n); try (InputStream in = zip.getInputStream(e)) { int x; while ((x=in.read(buffer))!=-1) out.write(buffer,0,x); } out.closeEntry();
                    }
                }
            }
            if (!found) throw new IOException("AAPT2 output has no resources.arsc");
            Files.move(tmp.toPath(), apk.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp.toPath()); }
    }
}
