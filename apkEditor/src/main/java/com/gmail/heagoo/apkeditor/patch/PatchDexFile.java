package com.gmail.heagoo.apkeditor.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stages an EXECUTE_DEX script so Android 14+ is willing to load it.
 *
 * <p>Since Android 14 a dynamically loaded DEX file must be read-only before it
 * is handed to a class loader. Shared storage cannot satisfy that reliably, so
 * the caller passes a private app directory ({@code Context.getDir(...,
 * MODE_PRIVATE)} or the code cache). The stream is fully written and closed
 * before the write bits are removed; loading a still-open or writable file is
 * exactly what produced {@code SecurityException: Writable dex file ... is not
 * allowed}.
 */
final class PatchDexFile {

    private PatchDexFile() {
    }

    /**
     * Copies {@code input} to a fresh read-only file inside {@code directory}.
     * The input is closed by this method. On any failure the partial file is
     * removed, so a later run cannot pick up a truncated script.
     */
    static File extract(File directory, InputStream input) throws IOException {
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("Private DEX directory is unavailable: " + directory);
        }
        File file = File.createTempFile("script-", ".dex", directory);
        boolean ready = false;
        try {
            try (InputStream in = input; OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }
            if (!file.setReadOnly() || file.canWrite()) {
                throw new IOException("Could not make DEX script read-only: " + file.getAbsolutePath());
            }
            ready = true;
            return file;
        } finally {
            if (!ready) {
                // Best effort: the directory is private and the name is unique.
                file.delete();
            }
        }
    }
}
