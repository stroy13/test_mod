package com.gmail.heagoo.apkeditor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Optimistic concurrency check for an editable decoded manifest. */
final class ManifestDocumentFile {
    static final class Conflict extends IOException { }
    private ManifestDocumentFile() { }

    static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    static void save(File file, String expected, String text) throws IOException {
        String disk = read(file);
        if (!disk.equals(expected) && !disk.equals(text)) throw new Conflict();
        if (!disk.equals(text)) AtomicProjectFile.writeText(file, text);
    }
}
