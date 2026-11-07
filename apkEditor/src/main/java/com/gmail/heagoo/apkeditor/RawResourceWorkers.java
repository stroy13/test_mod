package com.gmail.heagoo.apkeditor;

import brut.androlib.res.data.ResPackage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class RawResourceWorkers implements AutoCloseable {
    private static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
        @Override
        public byte[] initialValue() {
            return new byte[32768];
        }
    };
    private final ExecutorService pool;
    private final ThreadLocal<XmlDecoder> xml;
    private final ZipFile zip;

    RawResourceWorkers(File apk, final ApkDecoderMine owner, final ResPackage pkg, final boolean protectedApk) throws IOException {
        this.xml = new ThreadLocal<XmlDecoder>() {
            @Override
            public XmlDecoder initialValue() {
                XmlDecoder decoder = new XmlDecoder(owner, pkg);
                if (protectedApk) {
                    decoder.setApkProtected(true);
                }
                return decoder;
            }
        };
        this.zip = new ZipFile(apk);
        int count = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors()));
        this.pool = Executors.newFixedThreadPool(count);
    }

    Future<Void> submit(final String zipPath, final File output, final byte[] dummy) {
        return this.pool.submit(new Callable() {
            @Override
            public final Object call() throws Exception {
                return RawResourceWorkers.this.m262lambda$submit$0$comgmailheagooapkeditorRawResourceWorkers(dummy, output, zipPath);
            }
        });
    }

    Void m262lambda$submit$0$comgmailheagooapkeditorRawResourceWorkers(byte[] dummy, File output, String zipPath) throws Exception {
        if (dummy != null) {
            OutputStream out = new FileOutputStream(output);
            try {
                out.write(dummy);
                out.close();
                return null;
            } catch (Throwable th) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        ZipEntry entry = this.zip.getEntry(zipPath);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Missing ZIP entry: " + zipPath);
        }
        InputStream in = this.zip.getInputStream(entry);
        try {
            OutputStream out2 = new FileOutputStream(output);
            try {
                byte[] buffer = BUFFER.get();
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    out2.write(buffer, 0, count);
                }
                out2.close();
                if (in != null) {
                    in.close();
                    return null;
                }
                return null;
            } finally {
            }
        } catch (Throwable th3) {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable th4) {
                    th3.addSuppressed(th4);
                }
            }
            throw th3;
        }
    }

    Future<Void> submitXml(final String zipPath, final File output) {
        return this.pool.submit(new Callable() {
            @Override
            public final Object call() throws Exception {
                return RawResourceWorkers.this.m263lambda$submitXml$1$comgmailheagooapkeditorRawResourceWorkers(zipPath, output);
            }
        });
    }

    Void m263lambda$submitXml$1$comgmailheagooapkeditorRawResourceWorkers(String zipPath, File output) throws Exception {
        ZipEntry entry = this.zip.getEntry(zipPath);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Missing ZIP entry: " + zipPath);
        }
        try {
            InputStream in = this.zip.getInputStream(entry);
            try {
                OutputStream out = new FileOutputStream(output);
                try {
                    this.xml.get().decode(in, out);
                    out.close();
                    if (in != null) {
                        in.close();
                        return null;
                    }
                    return null;
                } finally {
                }
            } finally {
            }
        } catch (Exception ex) {
            this.xml.remove();
            throw ex;
        }
    }

    @Override
    public void close() throws IOException {
        this.pool.shutdown();
        boolean interrupted = false;
        while (true) {
            try {
                if (this.pool.awaitTermination(1L, TimeUnit.DAYS)) {
                    break;
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        try {
            this.zip.close();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
