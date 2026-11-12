package com.gmail.heagoo.apkeditor.translate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/** Imports GGUF files into app-private storage. Call on a background thread. */
public final class LocalModelStore {
    public interface Progress {
        boolean isCancelled();
        void onBytes(long bytes);
        default void onTotal(long bytes) {}
    }
    private LocalModelStore() {}

    /** The caller owns input; a valid header alone doesn't guarantee a working model. */
    public static File importModel(InputStream input, File directory) throws IOException {
        return importModel(input, directory, -1, null, null);
    }

    /** A known length/hash is verified before the temporary file is made visible. */
    public static File importModel(InputStream input, File directory, long expectedBytes,
                                   String sha256, Progress progress) throws IOException {
        if (input == null || directory == null) throw new IllegalArgumentException();
        if (progress != null) progress.onTotal(expectedBytes);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())
            throw new IOException("Cannot create model directory");
        File temp = File.createTempFile("import-", ".partial", directory);
        boolean committed = false;
        try {
            MessageDigest digest;
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException e) { throw new IOException(e); }
            BufferedInputStream in = new BufferedInputStream(input, 65536);
            byte[] header = new byte[8];
            int read = 0;
            while (read < header.length) {
                checkCancelled(progress);
                int n = in.read(header, read, header.length - read);
                if (n < 0) throw new IOException("Incomplete GGUF header");
                if (n == 0) continue;
                read += n;
            }
            int version = (header[4] & 255) | ((header[5] & 255) << 8)
                    | ((header[6] & 255) << 16) | ((header[7] & 255) << 24);
            if (header[0] != 'G' || header[1] != 'G' || header[2] != 'U'
                    || header[3] != 'F' || (version != 2 && version != 3))
                throw new IOException("Unsupported GGUF header or version");
            long count = header.length;
            digest.update(header);
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(header);
                byte[] buffer = new byte[65536];
                int n;
                while (true) {
                    checkCancelled(progress);
                    n = in.read(buffer);
                    if (n == -1) break;
                    if (n == 0) continue;
                    count += n;
                    if (expectedBytes >= 0 && count > expectedBytes) throw new IOException("Model size mismatch");
                    out.write(buffer, 0, n);
                    digest.update(buffer, 0, n);
                    if (progress != null) progress.onBytes(count);
                }
                if (expectedBytes >= 0 && count != expectedBytes) throw new IOException("Incomplete model download or import");
                if (sha256 != null && !sha256.equalsIgnoreCase(hex(digest.digest())))
                    throw new IOException("Model SHA-256 mismatch");
                checkCancelled(progress);
                out.getFD().sync();
            }
            checkCancelled(progress);
            File target = new File(directory, UUID.randomUUID().toString() + ".gguf");
            if (target.exists() || !temp.renameTo(target)) throw new IOException("Cannot store model");
            committed = true;
            return target;
        } finally {
            if (!committed) temp.delete();
        }
    }
    private static void checkCancelled(Progress progress) throws IOException {
        if (Thread.currentThread().isInterrupted() || (progress != null && progress.isCancelled()))
            throw new IOException("Import cancelled");
    }
    private static String hex(byte[] digest) {
        StringBuilder out = new StringBuilder(digest.length * 2);
        for (byte b : digest) out.append(Character.forDigit((b & 255) >>> 4, 16)).append(Character.forDigit(b & 15, 16));
        return out.toString();
    }
}
