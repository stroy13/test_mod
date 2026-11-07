package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ComparisonCore;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Set;

public final class ComparisonTextSave {
    public static File resolve(ComparisonCore.Result r, ComparisonCore.Entry e, boolean left) throws IOException {
        if (r.kind != ComparisonCore.Kind.ARCHIVES && !e.directory) {
            if ((left ? e.leftSize : e.rightSize) >= 0) {
                File root = left ? r.left : r.right;
                File source = root;
                if (r.kind == ComparisonCore.Kind.FOLDERS) {
                    for (String part : e.name.split("/")) {
                        if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                            throw new IOException("Недопустимый путь");
                        }
                        source = new File(source, part);
                        if (Files.isSymbolicLink(source.toPath())) {
                            throw new IOException("Символическая ссылка");
                        }
                    }
                    if (!source.getCanonicalFile().toPath().startsWith(root.getCanonicalFile().toPath())) {
                        throw new IOException("Путь вне папки");
                    }
                }
                if (!source.isFile() || Files.isSymbolicLink(source.toPath())) {
                    throw new IOException("Файл не найден или является ссылкой");
                }
                return source;
            }
        }
        throw new IOException("Этот объект нельзя сохранить");
    }

    public static void save(File file, String original, String replacement) throws IOException {
        if (original == null || replacement == null) {
            throw new IOException("Нет исходного текста");
        }
        if (Files.isSymbolicLink(file.toPath())) {
            throw new IOException("Символическая ссылка");
        }
        byte[] expected = original.getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = replacement.getBytes(StandardCharsets.UTF_8);
        if (newBytes.length > 524288) {
            throw new IOException("Текст больше 512 КБ");
        }
        if (file.length() > 524288 || !Arrays.equals(expected, Files.readAllBytes(file.toPath()))) {
            throw new IOException("Файл изменился после открытия — сравните заново");
        }
        Path parent = file.toPath().toAbsolutePath().getParent();
        Path temp = Files.createTempFile(parent, ".apkeditor-compare-", ".tmp", new FileAttribute[0]);
        try {
            try {
                Set<PosixFilePermission> mode = Files.getPosixFilePermissions(file.toPath(), new LinkOption[0]);
                Files.setPosixFilePermissions(temp, mode);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (UnsupportedOperationException e) {
        }
        Files.write(temp, newBytes, new OpenOption[0]);
        if (Files.isSymbolicLink(file.toPath()) || !Arrays.equals(expected, Files.readAllBytes(file.toPath()))) {
            throw new IOException("Файл изменился — сохранение отменено");
        }
        try {
            Files.move(temp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e2) {
            throw new IOException("Хранилище не поддерживает безопасную атомарную запись", e2);
        }
    }

    private ComparisonTextSave() {
    }
}
