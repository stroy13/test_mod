package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Repair only files consisting entirely of the decoder's legacy dummy entries. */
final class Aapt2DummyResources {
    private static final String TYPES = "anim|animator|drawable|layout|menu|xml|raw|font|mipmap|transition|interpolator|navigation";
    private static final String ITEM = "<item\\s+type=\"(?:" + TYPES + ")\"\\s+name=\"dummy_ae_[0-9a-f]+\"\\s*>false</item>";
    private static final Pattern ENTRY = Pattern.compile(ITEM);
    private static final long MAX_VALUES_BYTES = 4L * 1024 * 1024;

    private Aapt2DummyResources() { }

    static String normalize(String xml) {
        // Deliberately accept only the exact decoder-generated, line-oriented
        // document. Mixed or user-edited files are never rewritten.
        String[] lines = xml.split("\n", -1);
        boolean opened = false, closed = false, declaration = false;
        int count = 0;
        for (String line : lines) {
            String text = line.trim();
            if (text.isEmpty()) continue;
            if (!opened && !declaration && text.startsWith("<?xml ") && text.endsWith("?>")) {
                declaration = true;
            } else if (!opened && text.equals("<resources>")) {
                opened = true;
            } else if (opened && !closed && text.equals("</resources>")) {
                closed = true;
            } else if (opened && !closed && ENTRY.matcher(text).matches()) {
                count++;
            } else {
                return xml;
            }
        }
        if (!opened || !closed || count == 0) return xml;
        return xml.replace(">false</item>", ">@null</item>");
    }

    /** Return changed relative files; do not alter the project. */
    static Map<Path, byte[]> findRepairs(Path res) throws IOException {
        Map<Path, byte[]> changes = new HashMap<>();
        File[] dirs = res.toFile().listFiles();
        if (dirs == null) throw new IOException("Cannot list resources: " + res);
        for (File dir : dirs) {
            String name = dir.getName();
            if (!dir.isDirectory() || !(name.equals("values") || name.startsWith("values-"))) continue;
            File[] files = dir.listFiles();
            if (files == null) throw new IOException("Cannot list resources: " + dir);
            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".xml") || file.length() > MAX_VALUES_BYTES) continue;
                byte[] original = Files.readAllBytes(file.toPath());
                String xml = new String(original, StandardCharsets.UTF_8);
                String repaired = normalize(xml);
                if (!xml.equals(repaired)) {
                    changes.put(res.relativize(file.toPath()), repaired.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return changes;
    }

    static void stage(Path source, Path target, Map<Path, byte[]> repairs) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) throw new IOException("Unsupported resource file: " + file);
                Path relative = source.relativize(file);
                Path dest = target.resolve(relative);
                byte[] data = repairs.get(relative);
                if (data != null) Files.write(dest, data);
                else Files.copy(file, dest);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }
}
