package com.gmail.heagoo.apkeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/* loaded from: /tmp/apkx/classes5.dex */
public final class ProjectAnalysis {
    private static final BooleanSupplier NEVER = new BooleanSupplier() {
        @Override // java.util.function.BooleanSupplier
        public final boolean getAsBoolean() {
            return ProjectAnalysis.lambda$static$0();
        }
    };

    public interface Progress {
        void update(String str, int i, int i2, String str2);
    }

    public static final class Finding {
        public final int line;
        public final String message;
        public final String path;

        Finding(String p, int l, String m) {
            this.path = p;
            this.line = l;
            this.message = m;
        }

        public String toString() {
            return this.path + (this.line > 0 ? ":" + this.line : "") + " — " + this.message;
        }
    }

    public static final class Report {
        public int checked;
        public final List<Finding> findings = new ArrayList();
        public boolean resourceIndexIncomplete;
        public int skipped;
        public int smaliChecked;
        public int smaliNotParsed;
        public boolean smaliSyntaxUnavailable;
        public boolean truncated;
        public int xmlChecked;

        void add(String p, int l, String m) {
            if (this.findings.size() < 1000) {
                this.findings.add(new Finding(p, l, m));
            } else {
                this.truncated = true;
            }
        }
    }

    static boolean lambda$static$0() {
        return false;
    }

    private static void cancel(BooleanSupplier cancelled) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new InterruptedIOException("Проверка отменена");
        }
    }

    private static void progress(Progress listener, String stage, int done, int total, String path) {
        if (listener != null) {
            listener.update(stage, done, total, path);
        }
    }

    private static void walk(File root, List<File> out, Report report, Progress listener, BooleanSupplier cancelled) throws IOException {
        List<File> list;
        boolean z;
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        progress(listener, "Поиск файлов", 0, 0, "");
        while (!stack.isEmpty()) {
            cancel(cancelled);
            File dir = stack.pop();
            File[] files = dir.listFiles();
            boolean r9 = true;
            if (files == null) {
                report.skipped++;
            } else {
                Arrays.sort(files, Comparator.comparing(new Function() {
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((File) obj).getName();
                    }
                }));
                List<File> subdirs = new ArrayList<>();
                int length = files.length;
                int i = 0;
                while (i < length) {
                    File f = files[i];
                    cancel(cancelled);
                    if (out.size() >= 100000) {
                        report.truncated = r9;
                        return;
                    }
                    if (Files.isSymbolicLink(f.toPath())) {
                        report.skipped++;
                        z = r9;
                    } else {
                        if (f.isDirectory()) {
                            subdirs.add(f);
                            list = out;
                        } else if (f.isFile()) {
                            list = out;
                            list.add(f);
                        } else {
                            list = out;
                        }
                        visited++;
                        if (visited % 32 == 0) {
                            z = r9;
                            progress(listener, "Поиск файлов", list.size(), 0, relative(root, f));
                        } else {
                            z = r9;
                        }
                    }
                    i++;
                    r9 = z;
                }
                for (int i2 = subdirs.size() - 1; i2 >= 0; i2--) {
                    stack.push(subdirs.get(i2));
                }
            }
        }
    }

    private static String relative(File root, File f) {
        return root.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
    }

    private static boolean textAllowed(File f) {
        return f.length() <= 4194304;
    }

    private static BufferedReader reader(File f) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
    }

    public static Report resourceUses(File root, String id) throws Exception {
        return resourceUses(root, id, null, NEVER);
    }

    public static Report resourceUses(File root, String id, Progress listener, BooleanSupplier cancelled) throws Exception {
        String[] v;
        String[] v2;
        Throwable th;
        String str;
        File file = root;
        if (!id.matches("[A-Za-z_][A-Za-z0-9_]*/[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Укажите ресурс как type/name, например string/app_name");
        }
        String[] v3 = id.split("/");
        String[] strArr = null;
        boolean z = true;
        Pattern pat = Pattern.compile("(?<![A-Za-z0-9_])@(?:\\+)?" + Pattern.quote(v3[0]) + "/" + Pattern.quote(v3[1]) + "(?![A-Za-z0-9_])|R\\$" + Pattern.quote(v3[0]) + ";->" + Pattern.quote(v3[1]) + "(?=[:(])|R\\." + Pattern.quote(v3[0]) + "\\." + Pattern.quote(v3[1]) + "(?![A-Za-z0-9_])");
        Report r = new Report();
        List<File> files = new ArrayList<>();
        walk(file, files, r, listener, cancelled);
        List<File> selected = new ArrayList<>();
        for (File f : files) {
            cancel(cancelled);
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.endsWith(".xml") || n.endsWith(".smali") || n.endsWith(".java") || n.endsWith(".kt")) {
                selected.add(f);
            }
        }
        progress(listener, "Поиск использования", 0, selected.size(), "");
        int done = 0;
        for (File f2 : selected) {
            cancel(cancelled);
            boolean z2 = z;
            String path = relative(file, f2);
            if (textAllowed(f2)) {
                r.checked++;
                try {
                    BufferedReader in = reader(f2);
                    int no = 0;
                    while (true) {
                        try {
                            String line = in.readLine();
                            if (line == null) {
                                break;
                            }
                            int no2 = no + 1;
                            try {
                                if (no % BinaryDiffIndex.PAGE_ROWS == 0) {
                                    try {
                                        cancel(cancelled);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        v = v3;
                                        v2 = null;
                                        if (in != null) {
                                        }
                                        throw th;
                                    }
                                }
                                if (pat.matcher(line).find()) {
                                    String trimmed = line.trim();
                                    if (trimmed.length() > 300) {
                                        v = v3;
                                        v2 = null;
                                        try {
                                            str = trimmed.substring(0, 300) + "…";
                                        } catch (Throwable th3) {
                                            th = th3;
                                            if (in != null) {
                                                try {
                                                    in.close();
                                                } catch (Throwable th4) {
                                                    th.addSuppressed(th4);
                                                }
                                            }
                                            throw th;
                                        }
                                    } else {
                                        v = v3;
                                        v2 = null;
                                        str = trimmed;
                                    }
                                    r.add(path, no2, str);
                                } else {
                                    v = v3;
                                    v2 = null;
                                }
                                no = no2;
                                strArr = v2;
                                v3 = v;
                            } catch (Throwable th5) {
                                th = th5;
                                v = v3;
                                v2 = null;
                                th = th;
                                if (in != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            v = v3;
                            v2 = strArr;
                        }
                    }
                    v = v3;
                    v2 = strArr;
                    if (in != null) {
                        try {
                            in.close();
                        } catch (InterruptedIOException stop) {
                            throw stop;
                        } catch (IOException e) {
                            r.skipped++;
                            r.checked--;
                            done++;
                            progress(listener, "Поиск использования", done, selected.size(), path);
                            file = root;
                            strArr = v2;
                            z = z2;
                            v3 = v;
                        }
                    }
                } catch (InterruptedIOException stop2) {
                    throw stop2;
                } catch (IOException e2) {
                    v = v3;
                    v2 = strArr;
                }
            } else {
                r.skipped++;
                v = v3;
                v2 = strArr;
            }
            done++;
            progress(listener, "Поиск использования", done, selected.size(), path);
            file = root;
            strArr = v2;
            z = z2;
            v3 = v;
        }
        return r;
    }

    private static Set<String> resourceIndex(File root, List<File> files, Report report, Progress listener, BooleanSupplier cancelled) throws IOException {
        Iterator<File> it;
        int done;
        File f;
        boolean z;
        Pattern declaration;
        Pattern typed;
        BufferedReader in;
        Throwable th;
        Set<String> defs = new HashSet<>();
        Pattern declaration2 = Pattern.compile("<(string|color|dimen|bool|integer|style|attr|id|array|plurals|drawable|layout|menu|anim|animator|interpolator|font|fraction)\\s+[^>]*?name\\s*=\\s*[\"']([A-Za-z_][A-Za-z0-9_.]*)[\"']");
        Pattern typed2 = Pattern.compile("<(?:item|public)\\b([^>]*)>");
        Pattern attr = Pattern.compile("(type|name)\\s*=\\s*[\"']([^\"']+)[\"']");
        Pattern ids = Pattern.compile("@\\+id/([A-Za-z_][A-Za-z0-9_]*)");
        int i = 0;
        progress(listener, "Поиск ресурсов", 0, files.size(), "");
        Iterator<File> it2 = files.iterator();
        int done2 = 0;
        while (it2.hasNext()) {
            File f2 = it2.next();
            cancel(cancelled);
            String rel = relative(root, f2);
            int i2 = 2;
            int i3 = i;
            if (rel.startsWith("res/")) {
                String[] parts = rel.split("/");
                z = true;
                if (parts.length != 3 || parts[1].startsWith("values")) {
                    it = it2;
                    done = done2;
                    f = f2;
                } else {
                    String type = parts[1].split("-")[i3];
                    String name = parts[2];
                    it = it2;
                    int dot = name.indexOf(46);
                    if (dot > 0) {
                        done = done2;
                        StringBuilder append = new StringBuilder().append(type).append("/");
                        f = f2;
                        String type2 = name.substring(i3, dot);
                        defs.add(append.append(type2).toString());
                    } else {
                        done = done2;
                        f = f2;
                    }
                }
            } else {
                it = it2;
                done = done2;
                f = f2;
                z = true;
            }
            if (!rel.endsWith(".xml")) {
                declaration = declaration2;
                typed = typed2;
            } else if (!textAllowed(f)) {
                report.resourceIndexIncomplete = z;
                declaration = declaration2;
                typed = typed2;
            } else {
                try {
                    BufferedReader in2 = reader(f);
                    int lineCount = 0;
                    while (true) {
                        try {
                            String line = in2.readLine();
                            if (line == null) {
                                break;
                            }
                            int lineCount2 = lineCount + 1;
                            if (lineCount % BinaryDiffIndex.PAGE_ROWS == 0) {
                                try {
                                    cancel(cancelled);
                                } catch (Throwable th2) {
                                    declaration = declaration2;
                                    typed = typed2;
                                    in = in2;
                                    th = th2;
                                    if (in != null) {
                                        try {
                                            in.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    }
                                    throw th;
                                }
                            }
                            Matcher m = declaration2.matcher(line);
                            while (m.find()) {
                                try {
                                    declaration = declaration2;
                                    in = in2;
                                } catch (Throwable th4) {
                                    declaration = declaration2;
                                    in = in2;
                                    th = th4;
                                    typed = typed2;
                                }
                                try {
                                    defs.add(m.group(1) + "/" + m.group(i2));
                                    declaration2 = declaration;
                                    in2 = in;
                                    i2 = 2;
                                } catch (Throwable th5) {
                                    th = th5;
                                    typed = typed2;
                                    if (in != null) {
                                    }
                                    throw th;
                                }
                            }
                            declaration = declaration2;
                            in = in2;
                            try {
                                Matcher m2 = ids.matcher(line);
                                while (m2.find()) {
                                    defs.add("id/" + m2.group(1));
                                }
                                Matcher m3 = typed2.matcher(line);
                                while (m3.find()) {
                                    String type3 = m3.group(1);
                                    Matcher a = attr.matcher(type3);
                                    String name2 = null;
                                    String type4 = null;
                                    while (a.find()) {
                                        Matcher m4 = m3;
                                        typed = typed2;
                                        try {
                                            if (a.group(1).equals("type")) {
                                                type4 = a.group(2);
                                            } else {
                                                name2 = a.group(2);
                                            }
                                            typed2 = typed;
                                            m3 = m4;
                                        } catch (Throwable th6) {
                                            th = th6;
                                            if (in != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    Matcher m5 = m3;
                                    Pattern typed3 = typed2;
                                    if (type4 != null && name2 != null) {
                                        defs.add(type4 + "/" + name2);
                                    }
                                    typed2 = typed3;
                                    m3 = m5;
                                }
                                i2 = 2;
                                lineCount = lineCount2;
                                declaration2 = declaration;
                                in2 = in;
                            } catch (Throwable th7) {
                                typed = typed2;
                                th = th7;
                            }
                        } catch (Throwable th8) {
                            declaration = declaration2;
                            typed = typed2;
                            in = in2;
                            th = th8;
                        }
                    }
                    declaration = declaration2;
                    typed = typed2;
                    BufferedReader in3 = in2;
                    if (in3 != null) {
                        try {
                            in3.close();
                        } catch (InterruptedIOException stop) {
                            throw stop;
                        } catch (IOException e) {
                            report.resourceIndexIncomplete = true;
                            done2 = done + 1;
                            progress(listener, "Поиск ресурсов", done2, files.size(), rel);
                            declaration2 = declaration;
                            it2 = it;
                            typed2 = typed;
                            i = 0;
                        }
                    }
                } catch (InterruptedIOException stop2) {
                    throw stop2;
                } catch (IOException e2) {
                    declaration = declaration2;
                    typed = typed2;
                }
            }
            done2 = done + 1;
            progress(listener, "Поиск ресурсов", done2, files.size(), rel);
            declaration2 = declaration;
            it2 = it;
            typed2 = typed;
            i = 0;
        }
        return defs;
    }

    public static Report diagnose(File root) throws Exception {
        return diagnose(root, null, NEVER);
    }

    public static Report diagnose(File root, Progress listener, BooleanSupplier cancelled) throws Exception {
        Exception e = null;
        boolean z;
        int i;
        Iterator<File> it;
        int done = 0;
        Pattern reference = null;
        Throwable th;
        Throwable th2 = null;
        int api;
        int length;
        int api2;
        String xmlText = null;
        InputStream in;
        Throwable th3;
        BufferedReader in2;
        Throwable th4;
        File file = root;
        Report r = new Report();
        List<File> files = new ArrayList<>();
        walk(file, files, r, listener, cancelled);
        if (r.truncated || r.skipped > 0) {
            r.resourceIndexIncomplete = true;
        }
        Set<String> definitions = resourceIndex(file, files, r, listener, cancelled);
        Pattern reference2 = Pattern.compile("@([A-Za-z_][A-Za-z0-9_]*)/([A-Za-z_][A-Za-z0-9_.]*)");
        List<File> selected = new ArrayList<>();
        Iterator<File> it2 = files.iterator();
        boolean smaliPresent = false;
        while (smaliPresent) {
            File f = it2.next();
            cancel(cancelled);
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.endsWith(".xml") || n.endsWith(".smali")) {
                selected.add(f);
                if (n.endsWith(".smali")) {
                    smaliPresent = true;
                }
            }
        }
        Method smaliChecker = null;
        if (smaliPresent) {
            i = 0;
            z = true;
            try {
                smaliChecker = Class.forName("com.gmail.heagoo.apkeditor.pro.SmaliCheck").getMethod("check", String.class, Integer.TYPE);
            } catch (ClassNotFoundException e2) {
                r.smaliSyntaxUnavailable = z;
                progress(listener, "Проверка XML и smali", i, selected.size(), "");
                it = selected.iterator();
                int done2 = 0;
                while (it.hasNext()) {
                }
                return r;
            } catch (LinkageError e3) {
                r.smaliSyntaxUnavailable = z;
                progress(listener, "Проверка XML и smali", i, selected.size(), "");
                it = selected.iterator();
                int done22 = 0;
                while (it.hasNext()) {
                }
                return r;
            } catch (NoSuchMethodException e4) {
                r.smaliSyntaxUnavailable = z;
                progress(listener, "Проверка XML и smali", i, selected.size(), "");
                it = selected.iterator();
                int done222 = 0;
                while (it.hasNext()) {
                }
                return r;
            }
        } else {
            i = 0;
        }
        progress(listener, "Проверка XML и smali", i, selected.size(), "");
        it = selected.iterator();
        int done2222 = 0;
        while (it.hasNext()) {
            File f2 = it.next();
            cancel(cancelled);
            Iterator<File> it3 = it;
            String n2 = f2.getName().toLowerCase(Locale.ROOT);
            List<File> files2 = files;
            String p = relative(file, f2);
            if (textAllowed(f2)) {
                r.checked++;
                if (n2.endsWith(".xml")) {
                    r.xmlChecked++;
                    try {
                        byte[] xmlBytes = Files.readAllBytes(f2.toPath());
                        cancel(cancelled);
                        try {
                            xmlText = new String(xmlBytes, StandardCharsets.UTF_8);
                        } catch (Exception e6) {
                            e = e6;
                            done = done2222;
                            reference = reference2;
                        }
                    } catch (IOException e7) {
                        e = e7;
                        done = done2222;
                        reference = reference2;
                    } catch (Exception e8) {
                        e = e8;
                        done = done2222;
                        reference = reference2;
                    }
                    if (xmlText.indexOf(0) >= 0) {
                        done = done2222;
                        reference = reference2;
                    } else if (xmlText.contains("<!DOCTYPE")) {
                        done = done2222;
                        reference = reference2;
                    } else {
                        SAXParserFactory fac = SAXParserFactory.newInstance();
                        fac.setNamespaceAware(true);
                        try {
                            try {
                                fac.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                            } catch (Exception e9) {
                            }
                            try {
                                fac.setFeature("http://xml.org/sax/features/external-general-entities", false);
                            } catch (Exception e10) {
                            }
                            try {
                                fac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                            } catch (Exception e11) {
                            }
                            XMLReader xml = fac.newSAXParser().getXMLReader();
                            xml.setEntityResolver(new EntityResolver() {
                                @Override // org.xml.sax.EntityResolver
                                public final InputSource resolveEntity(String str, String str2) throws IOException, SAXException {
                                    return ProjectAnalysis.lambda$diagnose$1(str, str2);
                                }
                            });
                            xml.setErrorHandler(new DefaultHandler() {
                                @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                                public void error(SAXParseException e12) throws SAXException {
                                    throw e12;
                                }

                                @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                                public void fatalError(SAXParseException e12) throws SAXException {
                                    throw e12;
                                }
                            });
                            InputStream in3 = new FileInputStream(f2);
                            try {
                                in = in3;
                                try {
                                    xml.parse(new InputSource(in));
                                    in.close();
                                    BufferedReader in4 = reader(f2);
                                    int number = 0;
                                    while (true) {
                                        try {
                                            String line = in4.readLine();
                                            if (line == null) {
                                                break;
                                            }
                                            XMLReader xml2 = xml;
                                            int number2 = number + 1;
                                            try {
                                                if (number % BinaryDiffIndex.PAGE_ROWS == 0) {
                                                    try {
                                                        cancel(cancelled);
                                                    } catch (Throwable th5) {
                                                        th4 = th5;
                                                        in2 = in4;
                                                        if (in2 != null) {
                                                        }
                                                    }
                                                }
                                                String key = line;
                                                Matcher refs = reference2.matcher(key);
                                                while (refs.find()) {
                                                    String line2 = key;
                                                    in2 = in4;
                                                    Pattern reference3 = reference2;
                                                    Matcher refs2 = refs;
                                                    int done3 = done2222;
                                                    try {
                                                        String key2 = refs2.group(1) + "/" + refs2.group(2);
                                                        if (!definitions.contains(key2)) {
                                                            r.add(p, number2, "Возможно отсутствует ресурс @" + key2 + " (проверка приблизительная)");
                                                        }
                                                        key = line2;
                                                        done2222 = done3;
                                                        reference2 = reference3;
                                                        refs = refs2;
                                                        in4 = in2;
                                                    } catch (Throwable th6) {
                                                        th4 = th6;
                                                        if (in2 != null) {
                                                            throw th4;
                                                        }
                                                        try {
                                                            in2.close();
                                                            throw th4;
                                                        } catch (Throwable th7) {
                                                            th4.addSuppressed(th7);
                                                            throw th4;
                                                        }
                                                    }
                                                }
                                                number = number2;
                                                xml = xml2;
                                                in4 = in4;
                                            } catch (Throwable th8) {
                                                in2 = in4;
                                                th4 = th8;
                                            }
                                        } catch (Throwable th9) {
                                            in2 = in4;
                                            th4 = th9;
                                        }
                                    }
                                    BufferedReader in5 = in4;
                                    done = done2222;
                                    reference = reference2;
                                    if (in5 != null) {
                                        in5.close();
                                    }
                                } catch (Throwable th10) {
                                    th3 = th10;
                                    try {
                                        in.close();
                                        throw th3;
                                    } catch (Throwable th11) {
                                        th3.addSuppressed(th11);
                                        throw th3;
                                    }
                                }
                            } catch (Throwable th12) {
                                in = in3;
                                th3 = th12;
                            }
                        } catch (IOException stop3) {
                            throw stop3;
                        } catch (SAXParseException e12) {
                            done = done2222;
                            reference = reference2;
                            r.add(p, e12.getLineNumber(), "XML: " + e12.getMessage());
                            done2222 = done + 1;
                            progress(listener, "Проверка XML и smali", done2222, selected.size(), p);
                            file = root;
                            it = it3;
                            files = files2;
                            reference2 = reference;
                        }
                    }
                    r.add(p, 0, "XML не проверен: бинарная кодировка или DTD");
                    r.skipped++;
                    r.checked--;
                    r.xmlChecked--;
                } else {
                    done = done2222;
                    reference = reference2;
                    r.smaliChecked++;
                    if (smaliChecker == null) {
                        r.smaliNotParsed++;
                    }
                    if (smaliChecker != null) {
                        int api3 = 26;
                        try {
                            try {
                                File parent = f2.getParentFile();
                                while (parent != null && parent.toPath().startsWith(root.toPath())) {
                                    File meta = new File(parent, ".apkeditor-dex-api");
                                    if (meta.isFile()) {
                                        BufferedReader rd = reader(meta);
                                        try {
                                            api = Integer.parseInt(rd.readLine().trim());
                                            if (rd != null) {
                                                rd.close();
                                            }
                                            if (api >= 1 || api > 1000) {
                                                api = 26;
                                            }
                                            String[] output = (String[]) smaliChecker.invoke(null, f2.getAbsolutePath(), Integer.valueOf(api));
                                            length = output.length;
                                            api2 = 0;
                                            while (api2 < length) {
                                                String issue = output[api2];
                                                int i2 = api2;
                                                int c = issue.indexOf(58);
                                                int line3 = 0;
                                                File parent2 = parent;
                                                String[] output2 = output;
                                                try {
                                                    line3 = Integer.parseInt(issue.substring(0, c));
                                                } catch (Exception e13) {
                                                }
                                                r.add(p, line3, "smali: " + (c < 0 ? issue : issue.substring(c + 1)));
                                                api2 = i2 + 1;
                                                output = output2;
                                                parent = parent2;
                                            }
                                        } catch (Throwable th13) {
                                            if (rd != null) {
                                                try {
                                                    rd.close();
                                                } catch (Throwable th14) {
                                                    th13.addSuppressed(th14);
                                                }
                                            }
                                            throw th13;
                                        }
                                    } else {
                                        parent = parent.getParentFile();
                                        api3 = api3;
                                    }
                                }
                                api = api3;
                                if (api >= 1) {
                                }
                                api = 26;
                                String[] output3 = (String[]) smaliChecker.invoke(null, f2.getAbsolutePath(), Integer.valueOf(api));
                                length = output3.length;
                                api2 = 0;
                                while (api2 < length) {
                                }
                            } catch (Exception ex) {
                                r.smaliNotParsed++;
                                r.add(p, 0, "Не удалось проверить smali: " + String.valueOf(ex.getMessage()));
                            }
                        } catch (LinkageError e14) {
                            r.smaliSyntaxUnavailable = true;
                            smaliChecker = null;
                            r.smaliNotParsed++;
                        } catch (Throwable ex2) {
                            Throwable cause = ex2.getCause();
                            if (cause instanceof OutOfMemoryError) {
                                throw ((OutOfMemoryError) cause);
                            }
                            if (cause instanceof LinkageError) {
                                r.smaliSyntaxUnavailable = true;
                                smaliChecker = null;
                                r.smaliNotParsed++;
                            } else {
                                if (cause instanceof InterruptedIOException) {
                                    throw ((InterruptedIOException) cause);
                                }
                                r.smaliNotParsed++;
                                r.add(p, 0, "Не удалось проверить smali: " + String.valueOf(cause));
                            }
                        }
                    }
                    Throwable cause2 = null;
                    boolean hasClass = false;
                    int line4 = 0;
                    int methodLine = 0;
                    try {
                        BufferedReader in6 = reader(f2);
                        while (true) {
                            try {
                                String s = in6.readLine();
                                if (s == null) {
                                    break;
                                }
                                th = cause2;
                                int line5 = line4 + 1;
                                try {
                                    if (line4 % BinaryDiffIndex.PAGE_ROWS == 0) {
                                        cancel(cancelled);
                                    }
                                    String s2 = s.trim();
                                    if (s2.startsWith(".class ")) {
                                        hasClass = true;
                                    }
                                    if (s2.startsWith(".method ")) {
                                        if (th != null) {
                                            r.add(p, line5, "Вложенное .method; проверьте .end method");
                                        }
                                        methodLine = line5;
                                        line4 = line5;
                                        cause2 = new Throwable("in-method");
                                    } else if (s2.equals(".end method")) {
                                        if (th == null) {
                                            r.add(p, line5, ".end method без .method");
                                        }
                                        line4 = line5;
                                        cause2 = null;
                                    } else {
                                        line4 = line5;
                                        cause2 = th;
                                    }
                                } catch (Throwable th15) {
                                    th2 = th15;
                                }
                            } catch (Throwable th16) {
                                th = cause2;
                                th2 = th16;
                            }
                            if (in6 != null) {
                                try {
                                    in6.close();
                                } catch (Throwable th17) {
                                    th2.addSuppressed(th17);
                                }
                            }
                            throw new Exception(th2);
                        }
                        th = cause2;
                        if (th != null) {
                            try {
                                r.add(p, methodLine, "Метод не закрыт .end method");
                            } catch (Throwable th18) {
                                th2 = th18;
                            }
                        }
                        if (!hasClass) {
                            r.add(p, 1, "Не найдена директива .class");
                        }
                        if (in6 != null) {
                            try {
                                in6.close();
                            } catch (InterruptedIOException stop4) {
                                throw stop4;
                            } catch (IOException e15) {
                                e = e15;
                                cause2 = th;
                                r.add(p, 0, "Не удалось прочитать: " + e.getMessage());
                                r.skipped++;
                                r.checked--;
                                r.smaliChecked--;
                                done2222 = done + 1;
                                progress(listener, "Проверка XML и smali", done2222, selected.size(), p);
                                file = root;
                                it = it3;
                                files = files2;
                                reference2 = reference;
                            }
                        }
                    } catch (InterruptedIOException stop5) {
                        throw stop5;
                    } catch (IOException e16) {
                        e = e16;
                    }
                }
                done2222 = done + 1;
                progress(listener, "Проверка XML и smali", done2222, selected.size(), p);
                file = root;
                it = it3;
                files = files2;
                reference2 = reference;
            } else {
                r.skipped++;
                done2222++;
                progress(listener, "Проверка XML и smali", done2222, selected.size(), p);
                it = it3;
                files = files2;
            }
        }
        return r;
    }

    static InputSource lambda$diagnose$1(String publicId, String systemId) throws IOException, SAXException {
        return new InputSource(new StringReader(""));
    }

    private ProjectAnalysis() {
    }
}
