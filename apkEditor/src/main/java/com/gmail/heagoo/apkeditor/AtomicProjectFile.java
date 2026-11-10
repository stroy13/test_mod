package com.gmail.heagoo.apkeditor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Write a complete file before replacing the previous version. */
final class AtomicProjectFile {
    private AtomicProjectFile() { }

    interface Writer { void write(File file) throws IOException; }

    static void replace(File target, Writer writer) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null || !parent.isDirectory()) throw new IOException("Missing directory: " + parent);
        File temp = File.createTempFile(".apkeditor-", ".tmp", parent);
        try {
            writer.write(temp);
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temp.exists()) temp.delete();
        }
    }

    static void writeText(File target, String text) throws IOException {
        replace(target, file -> {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
        });
    }

    static void writeObject(File target, Serializable value) throws IOException {
        replace(target, file -> {
            try (FileOutputStream out = new FileOutputStream(file);
                 ObjectOutputStream objects = new ObjectOutputStream(out)) {
                objects.writeObject(value);
                objects.flush();
                out.getFD().sync();
            }
        });
    }
}
