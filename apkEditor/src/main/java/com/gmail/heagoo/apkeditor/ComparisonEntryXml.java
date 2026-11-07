package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ComparisonCore;
import com.gmail.heagoo.apklib.AXMLPrinter;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ComparisonEntryXml {
    private static final int LIMIT = 4194304;

    private static final class Limited extends OutputStream {
        final ByteArrayOutputStream out;
        boolean overflow;

        private Limited() {
            this.out = new ByteArrayOutputStream();
        }

        @Override
        public void write(int v) throws IOException {
            if (this.out.size() >= ComparisonEntryXml.LIMIT) {
                this.overflow = true;
            } else {
                this.out.write(v);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len > ComparisonEntryXml.LIMIT - this.out.size()) {
                this.overflow = true;
            } else {
                this.out.write(b, off, len);
            }
        }
    }

    public static boolean isBinary(ComparisonCore.Result result, ComparisonCore.Entry e, boolean side) throws IOException {
        InputStream raw = ComparisonCore.openInput(result, e, side);
        boolean z = false;
        if (raw == null) {
            if (raw != null) {
                raw.close();
            }
            return false;
        }
        try {
            byte[] h = new byte[4];
            int p = 0;
            while (p < 4) {
                int n = raw.read(h, p, 4 - p);
                if (n <= 0) {
                    break;
                }
                p += n;
            }
            if (p == 4 && (h[0] & ParserRule.MATCH_TYPE_CONTEXT) == 3 && h[1] == 0 && h[2] == 8) {
                if (h[3] == 0) {
                    z = true;
                }
            }
            if (raw != null) {
                raw.close();
            }
            return z;
        } catch (Throwable th) {
            if (raw != null) {
                try {
                    raw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static String read(ComparisonCore.Result result, ComparisonCore.Entry entry, boolean side) throws Exception {
        if ((side ? entry.leftSize : entry.rightSize) < 0) {
            return null;
        }
        if (!isBinary(result, entry, side)) {
            return ComparisonCore.readText(result, entry, side);
        }
        if ((side ? entry.leftSize : entry.rightSize) > 4194304) {
            throw new IOException("Бинарный XML больше 4 МБ");
        }
        InputStream raw = ComparisonCore.openInput(result, entry, side);
        try {
            ByteArrayOutputStream input = new ByteArrayOutputStream();
            byte[] b = new byte[ParserRule.END_REGEXP];
            while (true) {
                int n = raw.read(b);
                if (n == -1) {
                    Limited out = new Limited();
                    if (!new AXMLPrinter().convert(new ByteArrayInputStream(input.toByteArray()), out) || out.overflow) {
                        throw new IOException("Не удалось декодировать бинарный XML или XML больше 4 МБ");
                    }
                    String str = new String(out.out.toByteArray(), StandardCharsets.UTF_8);
                    if (raw != null) {
                        raw.close();
                    }
                    return str;
                }
                if (input.size() + n > LIMIT) {
                    throw new IOException("Бинарный XML больше 4 МБ");
                }
                input.write(b, 0, n);
            }
        } catch (Throwable th) {
            if (raw != null) {
                try {
                    raw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private ComparisonEntryXml() {
    }
}
