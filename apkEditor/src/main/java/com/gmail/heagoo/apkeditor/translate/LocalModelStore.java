package com.gmail.heagoo.apkeditor.translate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Imports GGUF files into app-private storage. Call on a background thread. */
public final class LocalModelStore {
    private LocalModelStore() {}

    /** The caller owns and closes input. Never overwrites an existing model.
     * A valid header does not imply a complete file or a supported architecture. */
    public static File importModel(InputStream input, File directory) throws IOException {
        if (input == null || directory == null) throw new IllegalArgumentException();
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create model directory");
        }
        File temp = File.createTempFile("import-", ".partial", directory);
        boolean committed = false;
        try {
            BufferedInputStream in = new BufferedInputStream(input, 65536);
            byte[] header = new byte[8];
            int read = 0;
            while (read < header.length) {
                int n = in.read(header, read, header.length - read);
                if (n < 0) throw new IOException("Incomplete GGUF header");
                if (n == 0) continue;
                read += n;
            }
            int version = (header[4] & 255) | ((header[5] & 255) << 8)
                    | ((header[6] & 255) << 16) | ((header[7] & 255) << 24);
            if (header[0] != 'G' || header[1] != 'G' || header[2] != 'U'
                    || header[3] != 'F' || (version != 2 && version != 3)) {
                throw new IOException("Unsupported GGUF header or version");
            }
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(header);
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Import cancelled");
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
            }
            File target = new File(directory, UUID.randomUUID().toString() + ".gguf");
            if (target.exists() || !temp.renameTo(target)) throw new IOException("Cannot store model");
            committed = true;
            return target;
        } finally {
            if (!committed) temp.delete();
        }
    }
}
