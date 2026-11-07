package com.gmail.heagoo.apkeditor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class CheckpointFiles {
    public static void copy(File source, File dest) throws IOException {
        if (Files.isSymbolicLink(source.toPath())) {
            throw new IOException("Символическая ссылка: " + source);
        }
        if (source.isDirectory()) {
            String a = source.getCanonicalPath() + File.separator;
            String b = dest.getCanonicalPath() + File.separator;
            if (b.startsWith(a)) {
                throw new IOException("Копия внутри исходной папки");
            }
            if (!dest.mkdirs() && !dest.isDirectory()) {
                throw new IOException("Не создать папку: " + dest);
            }
            File[] children = source.listFiles();
            if (children == null) {
                throw new IOException("Нет доступа: " + source);
            }
            for (File f : children) {
                copy(f, new File(dest, f.getName()));
            }
            return;
        }
        if (source.isFile()) {
            long size = source.length();
            long modified = source.lastModified();
            File parent = dest.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Не создать папку: " + parent);
            }
            InputStream in = new BufferedInputStream(new FileInputStream(source));
            try {
                FileOutputStream raw = new FileOutputStream(dest);
                try {
                    OutputStream out = new BufferedOutputStream(raw);
                    try {
                        byte[] buf = new byte[65536];
                        while (true) {
                            int n = in.read(buf);
                            if (n == -1) {
                                break;
                            } else {
                                out.write(buf, 0, n);
                            }
                        }
                        out.flush();
                        raw.getFD().sync();
                        out.close();
                        raw.close();
                        in.close();
                        if (size != dest.length() || size != source.length() || modified != source.lastModified()) {
                            throw new IOException("Файл изменился во время копирования: " + source);
                        }
                        return;
                    } finally {
                    }
                } finally {
                }
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        throw new IOException("Нет файла: " + source);
    }

    public static void delete(File f) {
        if (Files.isSymbolicLink(f.toPath())) {
            f.delete();
            return;
        }
        File[] c = f.listFiles();
        if (c != null) {
            for (File x : c) {
                delete(x);
            }
        }
        f.delete();
    }

    public static String rebase(String value, String oldDir, String newDir) throws IOException {
        if (value == null) {
            return null;
        }
        String old = new File(oldDir).getCanonicalPath();
        String v = new File(value).getCanonicalPath();
        if (v.equals(old)) {
            return new File(newDir).getCanonicalPath();
        }
        if (v.startsWith(old + File.separator)) {
            return new File(newDir).getCanonicalPath() + v.substring(old.length());
        }
        throw new IOException("Внешний путь в контрольной точке: " + value);
    }
}
