package com.gmail.heagoo.apkeditor;

import android.content.Context;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.decoder.ResourceIdProvider;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import com.gmail.heagoo.apklib.AXMLPrinter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: /tmp/apkx/classes5.dex */
public final class BinaryResourceComparison {
    private static final long INPUT_LIMIT = 536870912;

    private static void check(File f) throws IOException {
        if (!f.isFile() || Files.isSymbolicLink(f.toPath()) || f.length() > INPUT_LIMIT) {
            throw new IOException("Файл отсутствует, является ссылкой или больше 512 МБ: " + f);
        }
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static void write(File out, String data) throws IOException {
        File parent = out.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Не создать папку: " + parent);
        }
        if (out.exists()) {
            throw new IOException("Повтор имени ресурса: " + out);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8);
        try {
            w.write(data);
            w.close();
        } catch (Throwable th) {
            try {
                w.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static void xml(File input, File outputDir) throws Exception {
        check(input);
        byte[] head = new byte[4];
        InputStream in = new FileInputStream(input);
        try {
            int n = in.read(head);
            if (n < 4) {
                throw new IOException("Слишком короткий XML");
            }
            in.close();
            File out = new File(outputDir, "document.xml");
            if ((head[0] & 255) != 3 || head[1] != 0 || head[2] != 8 || head[3] != 0) {
                if (input.length() > 8388608) {
                    throw new IOException("Текстовый XML больше 8 МБ");
                }
                byte[] data = Files.readAllBytes(input.toPath());
                write(out, new String(data, StandardCharsets.UTF_8));
                return;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            InputStream in2 = new FileInputStream(input);
            try {
                if (!new AXMLPrinter().convert(in2, bytes)) {
                    throw new IOException("Не удалось декодировать бинарный XML");
                }
                in2.close();
                write(out, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            } catch (Throwable th) {
                try {
                    in2.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Throwable th3) {
            try {
                in.close();
            } catch (Throwable th4) {
                th3.addSuppressed(th4);
            }
            throw th3;
        }
    }

    public static void arscOrApk(Context ctx, File input, File outputDir, File scratch) throws Exception {
        Throwable th = null;
        if (!input.isFile() || Files.isSymbolicLink(input.toPath())) {
            throw new IOException("Файл не найден или является ссылкой");
        }
        if (!input.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
            arsc(ctx, input, outputDir);
            return;
        }
        File temp = File.createTempFile("resource-table-", ".arsc", scratch);
        try {
            try {
                ZipFile zip = new ZipFile(input);
                try {
                    try {
                        Enumeration<? extends ZipEntry> it = zip.entries();
                        ZipEntry found = null;
                        while (it.hasMoreElements()) {
                            ZipEntry e = it.nextElement();
                            if (e.getName().equals("resources.arsc")) {
                                if (found != null) {
                                    throw new IOException("Повтор resources.arsc в APK");
                                }
                                found = e;
                            }
                        }
                        try {
                            if (found == null || found.isDirectory()) {
                                throw new IOException("В APK нет resources.arsc");
                            }
                            if (found.getSize() >= 0) {
                                long size = found.getSize();
                                long j = INPUT_LIMIT;
                                if (size <= INPUT_LIMIT) {
                                    InputStream in = zip.getInputStream(found);
                                    try {
                                        OutputStream out = new BufferedOutputStream(new FileOutputStream(temp), 65536);
                                        try {
                                            byte[] buffer = new byte[65536];
                                            long n = 0;
                                            while (true) {
                                                int k = in.read(buffer);
                                                long j2 = j;
                                                if (k == -1) {
                                                    break;
                                                }
                                                n += k;
                                                if (n > j2) {
                                                    break;
                                                }
                                                out.write(buffer, 0, k);
                                                j = j2;
                                            }
                                        } catch (Throwable thIn) {
                                            try {
                                                try {
                                                    out.close();
                                                    throw thIn;
                                                } catch (Throwable th2) {
                                                    thIn.addSuppressed(th2);
                                                    throw thIn;
                                                }
                                            } catch (Throwable th3) {
                                                thIn = th3;
                                                Throwable th4 = thIn;
                                                if (in == null) {
                                                    throw th4;
                                                }
                                                try {
                                                    in.close();
                                                    throw th4;
                                                } catch (Throwable th5) {
                                                    th4.addSuppressed(th5);
                                                    throw th4;
                                                }
                                            }
                                        }
                                    } catch (Throwable th6) {
                                        th = th6;
                                    }
                                }
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            Throwable th8 = th;
                            try {
                                zip.close();
                                throw th8;
                            } catch (Throwable th9) {
                                th8.addSuppressed(th9);
                                throw th8;
                            }
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        temp.delete();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                }
            } catch (Throwable th12) {
                th = th12;
                temp.delete();
                throw th;
            }
        } catch (Throwable th13) {
            th = th13;
        }
    }

    public static void xmlOrApk(File input, File outputDir) throws Exception {
        Throwable th = null;
        Enumeration<? extends ZipEntry> it = null;
        String str;
        int count;
        int count2;
        Throwable th2 = null;
        byte[] head = null;
        int n = 0;
        String str2;
        String str3 = "/";
        if (!input.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
            xml(input, outputDir);
            return;
        }
        if (!input.isFile() || Files.isSymbolicLink(input.toPath())) {
            throw new IOException("APK отсутствует или является ссылкой");
        }
        long total = 0;
        int count3 = 0;
        Set<String> names = new HashSet<>();
        ZipFile zip = new ZipFile(input);
        try {
            it = zip.entries();
        } catch (Throwable th3) {
            th = th3;
        }
        loop0: while (it.hasMoreElements()) {
            ZipEntry entry = it.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()) {
                str = str3;
            } else {
                if (name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                    if (!names.add(name)) {
                        throw new IOException("Повтор XML в APK: " + name);
                    }
                    if (!name.startsWith(str3) && name.indexOf(92) < 0) {
                        int i = 0;
                        if (name.indexOf(0) < 0) {
                            String[] split = name.split(str3, -1);
                            int length = split.length;
                            int i2 = 0;
                            while (i2 < length) {
                                String part = split[i2];
                                if (!part.isEmpty()) {
                                    int i3 = i;
                                    if (!part.equals(".") && !part.equals("..")) {
                                        i2++;
                                        i = i3;
                                    }
                                }
                                throw new IOException("Некорректный путь XML: " + name);
                            }
                            int i4 = i;
                            int count4 = count3 + 1;
                            if (count4 > 50000) {
                                count = count4;
                                throw new IOException("Слишком много XML в APK");
                            }
                            try {
                                if (entry.getSize() < 0) {
                                    throw new IOException("Размер XML неизвестен: " + name);
                                }
                                InputStream in = new BufferedInputStream(zip.getInputStream(entry));
                                try {
                                    head = new byte[4];
                                    n = 0;
                                    while (n < 4) {
                                        try {
                                            int k = in.read(head, n, 4 - n);
                                            if (k <= 0) {
                                                break;
                                            } else {
                                                n += k;
                                            }
                                        } catch (Throwable th4) {
                                            count2 = count4;
                                            th2 = th4;
                                        }
                                    }
                                } catch (Throwable th5) {
                                    count2 = count4;
                                    th2 = th5;
                                }
                                try {
                                    if (n != 4 || (head[i4] & 255) != 3 || head[1] != 0 || head[2] != 8) {
                                        str2 = str3;
                                        count = count4;
                                    } else {
                                        if (head[3] == 0) {
                                            if (entry.getSize() <= 8388608) {
                                                ByteArrayOutputStream data = new ByteArrayOutputStream();
                                                data.write(head);
                                                byte[] buf = new byte[16384];
                                                long size = 4;
                                                while (true) {
                                                    try {
                                                        int k2 = in.read(buf);
                                                        String str4 = str3;
                                                        if (k2 == -1) {
                                                            break;
                                                        }
                                                        size += k2;
                                                        long total2 = total + k2;
                                                        if (size > 8388608 || total2 > 268435456) {
                                                            break loop0;
                                                        }
                                                        int i5 = i4;
                                                        try {
                                                            data.write(buf, i5, k2);
                                                            i4 = i5;
                                                            total = total2;
                                                            str3 = str4;
                                                        } catch (Throwable th6) {
                                                            th2 = th6;
                                                            count2 = count4;
                                                            break;
                                                        }
                                                    } catch (Throwable th7) {
                                                        count2 = count4;
                                                        th2 = th7;
                                                    }
                                                }
                                            } else {
                                                count2 = count4;
                                                try {
                                                    throw new IOException("XML в APK больше 8 МБ: " + name);
                                                } catch (Throwable th8) {
                                                    th2 = th8;
                                                }
                                            }
                                            in.close();
                                            if (th2 != null) {
                                                throw th2;
                                            }
                                        }
                                        str2 = str3;
                                        count = count4;
                                    }
                                    in.close();
                                    count3 = count;
                                    str3 = str2;
                                } catch (Throwable th10) {
                                    th = th10;
                                }
                            } catch (Throwable th11) {
                                th = th11;
                            }
                            if (th != null) {
                                try {
                                    zip.close();
                                } catch (Throwable th12) {
                                    th.addSuppressed(th12);
                                }
                                throw (th instanceof Exception) ? (Exception) th : new Exception(th);
                            }
                            zip.close();
                        }
                    }
                    throw new IOException("Некорректный путь XML: " + name);
                }
                str = str3;
            }
            str3 = str;
        }
        zip.close();
    }

    public static void arsc(Context ctx, File input, File outputDir) throws Exception {
        Iterator<ResResource> it;
        String value;
        int count;
        String safe;
        check(input);
        int i = 0;
        ResTable table = new ResTable(ctx, false);
        InputStream in = new BufferedInputStream(new FileInputStream(input));
        try {
            ResPackage[] packages = ARSCDecoder.decode(in, false, false, table, (ResourceIdProvider) null, false).getPackages();
            in.close();
            for (ResPackage p : packages) {
                table.addPackage(p, true);
            }
            int count2 = 0;
            int length = packages.length;
            while (i < length) {
                ResPackage pkg = packages[i];
                for (ResResSpec spec : pkg.listResSpecs()) {
                    String dir = safe(pkg.getName()) + "/" + safe(spec.getType().getName()) + "/" + safe(spec.getName());
                    String meta = "ID: " + spec.getId() + "\nPackage: " + pkg.getName() + "\nType: " + spec.getType().getName() + "\nName: " + spec.getName() + "\n";
                    ResPackage[] packages2 = packages;
                    int i2 = i;
                    write(new File(outputDir, dir + "/entry.txt"), meta);
                    List<ResResource> items = new ArrayList<>(spec.listResources());
                    items.sort(Comparator.comparing(new Function() {
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String qualifiers;
                            qualifiers = ((ResResource) obj).getConfig().getFlags().getQualifiers();
                            return qualifiers;
                        }
                    }));
                    Iterator<ResResource> it2 = items.iterator();
                    while (it2.hasNext()) {
                        ResResource res = it2.next();
                        int count3 = count2 + 1;
                        List<ResResource> items2 = items;
                        if (count3 > 100000) {
                            throw new IOException("Слишком много ресурсов");
                        }
                        String qualifier = res.getConfig().getFlags().getQualifiers();
                        if (res.getValue() instanceof ResFileValue) {
                            it = it2;
                            value = "file: " + ((brut.androlib.res.data.value.ResFileValue) res.getValue()).getPath();
                        } else {
                            it = it2;
                            if (!(res.getValue() instanceof ResValuesXmlSerializable)) {
                                throw new IOException("Неподдерживаемое значение ресурса " + spec.getFullName() + ": " + res.getValue().getClass().getSimpleName());
                            }
                            StringWriter output = new StringWriter();
                            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
                            serializer.setOutput(output);
                            ((brut.androlib.res.xml.ResValuesXmlSerializable) res.getValue()).serializeToResValuesXml(serializer, res);
                            serializer.flush();
                            value = output.toString();
                        }
                        StringBuilder append = new StringBuilder().append(dir).append("/");
                        if (qualifier.isEmpty()) {
                            count = count3;
                            safe = ApkComposeActivity.PRIMARY_NOTIF_CHANNEL;
                        } else {
                            count = count3;
                            safe = safe(qualifier);
                        }
                        write(new File(outputDir, append.append(safe).append(".xml").toString()), "Конфигурация: " + (qualifier.isEmpty() ? ApkComposeActivity.PRIMARY_NOTIF_CHANNEL : qualifier) + "\n" + value + "\n");
                        items = items2;
                        it2 = it;
                        count2 = count;
                    }
                    packages = packages2;
                    i = i2;
                }
                i++;
            }
        } finally {
        }
    }

    private BinaryResourceComparison() {
    }
}
