package com.gmail.heagoo.apkeditor.translate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads over HTTPS, including redirects, validating the final content before import. */
public final class LocalModelDownload {
    private LocalModelDownload() {}
    public static File download(String link, String sha256, File directory, LocalModelStore.Progress progress) throws IOException {
        if (sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) throw new IOException("Missing model checksum");
        URL url = new URL(link);
        for (int redirects = 0; redirects <= 8; redirects++) {
            if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IOException("HTTPS required");
            if (progress != null && progress.isCancelled()) throw new IOException("Download cancelled");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(30000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept-Encoding", "identity");
            try {
                int status = c.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = c.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect without location");
                    url = new URL(url, location);
                    continue;
                }
                if (status != 200) throw new IOException("Download HTTP " + status);
                long size = c.getContentLengthLong();
                try (InputStream in = c.getInputStream()) {
                    return LocalModelStore.importModel(in, directory, size, sha256, progress);
                }
            } finally { c.disconnect(); }
        }
        throw new IOException("Too many redirects");
    }
}
