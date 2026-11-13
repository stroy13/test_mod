package com.gmail.heagoo.apkeditor.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Incremental UTF-8 line reader for a growing build log. Used off the UI thread. */
public final class BuildLogTail {
    private long offset;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    public List<String> read(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            if (in.length() < offset) { offset = 0; pending.reset(); lines.add("\u0000RESET"); }
            in.seek(offset);
            byte[] data = new byte[65536];
            int remaining = 262144;
            int n;
            while (remaining > 0 && (n = in.read(data, 0, Math.min(remaining, data.length))) > 0) {
                offset += n; remaining -= n;
                int start = 0;
                for (int i = 0; i < n; i++) if (data[i] == '\n') {
                    pending.write(data, start, i - start);
                    String line = new String(pending.toByteArray(), StandardCharsets.UTF_8);
                    lines.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
                    pending.reset(); start = i + 1;
                }
                pending.write(data, start, n - start);
            }
        }
        return lines;
    }
}
