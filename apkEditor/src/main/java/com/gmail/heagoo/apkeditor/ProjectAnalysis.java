package com.gmail.heagoo.apkeditor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** On-disk project diagnostics. The optional full smali parser lives in the Pro module. */
public final class ProjectAnalysis {
    static final int MAX_FILES = 100000;
    static final int MAX_TEXT_BYTES = 4 * 1024 * 1024;
    static final int MAX_FINDINGS = 1000;
    static final int DEFAULT_SMALI_API = 26;
    static final int MAX_SMALI_API = 1000;
    private static final int CANCEL_CHECK_LINES = 128;

    private static final BooleanSupplier NEVER = () -> false;
    private static final Pattern RESOURCE_REFERENCE =
            Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z_][A-Za-z0-9_]*)/([A-Za-z_][A-Za-z0-9_.]*)");
    private static final Pattern RESOURCE_DECLARATION = Pattern.compile(
            "<(string|color|dimen|bool|integer|style|attr|id|array|string-array|integer-array|plurals|"
                    + "drawable|layout|menu|anim|animator|interpolator|font|fraction)\\b[^>]*?\\bname\\s*=\\s*[\"']([A-Za-z_][A-Za-z0-9_.]*)[\"']");
    private static final Pattern TYPED_ITEM = Pattern.compile("<(?:item|public)\\b([^>]*)>");
    private static final Pattern ITEM_ATTRIBUTE = Pattern.compile("(type|name)\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern PLUS_ID = Pattern.compile("@\\+id/([A-Za-z_][A-Za-z0-9_]*)");

    public interface Progress {
        void update(String stage, int done, int total, String path);
    }

    public static final class Finding {
        public final int line;
        public final String message;
        public final String path;

        Finding(String path, int line, String message) {
            this.path = path;
            this.line = line;
            this.message = message;
        }

        @Override
        public String toString() {
            return this.path + (this.line > 0 ? ":" + this.line : "") + " — " + this.message;
        }
    }

    public static final class Report {
        public int checked;
        public int xmlFound;
        public int smaliFound;
        public final List<Finding> findings = new ArrayList<>();
        public boolean resourceIndexIncomplete;
        public int skipped;
        public int smaliChecked;
        public int smaliNotParsed;
        public boolean smaliSyntaxUnavailable;
        public boolean truncated;
        public int xmlChecked;

        void add(String path, int line, String message) {
            if (this.findings.size() < MAX_FINDINGS) {
                this.findings.add(new Finding(path, line, message));
            } else {
                this.truncated = true;
            }
        }
    }

    private ProjectAnalysis() {
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

    private static File canonicalRoot(File root) throws IOException {
        if (root == null) {
            throw new IOException("Каталог проекта не указан");
        }
        File canonical = root.getCanonicalFile();
        if (!canonical.isDirectory()) {
            throw new IOException("Каталог проекта недоступен: " + root.getPath());
        }
        return canonical;
    }

    private static String relative(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    private static boolean textAllowed(File file) {
        long length = file.length();
        return length >= 0 && length <= MAX_TEXT_BYTES;
    }

    private static BufferedReader reader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }

    private static List<File> walk(File root, Report report, Progress listener, BooleanSupplier cancelled)
            throws IOException {
        List<File> out = new ArrayList<>();
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        progress(listener, "Поиск файлов", 0, 0, "");
        while (!stack.isEmpty()) {
            cancel(cancelled);
            File dir = stack.pop();
            File[] children = dir.listFiles();
            if (children == null) {
                report.skipped++;
                continue;
            }
            Arrays.sort(children, Comparator.comparing(File::getName));
            List<File> subdirs = new ArrayList<>();
            for (File file : children) {
                cancel(cancelled);
                if (Files.isSymbolicLink(file.toPath())) {
                    report.skipped++;
                    continue;
                }
                if (file.isDirectory()) {
                    subdirs.add(file);
                } else if (file.isFile()) {
                    if (out.size() >= MAX_FILES) {
                        report.truncated = true;
                        return out;
                    }
                    out.add(file);
                } else {
                    report.skipped++;
                }
                visited++;
                if (visited % 32 == 0) {
                    progress(listener, "Поиск файлов", out.size(), 0, relative(root, file));
                }
            }
            for (int i = subdirs.size() - 1; i >= 0; i--) {
                stack.push(subdirs.get(i));
            }
        }
        progress(listener, "Поиск файлов", out.size(), 0, "");
        return out;
    }

    public static Report resourceUses(File root, String id) throws Exception {
        return resourceUses(root, id, null, NEVER);
    }

    public static Report resourceUses(File root, String id, Progress listener, BooleanSupplier cancelled)
            throws Exception {
        Objects.requireNonNull(id, "id");
        BooleanSupplier stop = cancelled == null ? NEVER : cancelled;
        if (!id.matches("[A-Za-z_][A-Za-z0-9_]*/[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Укажите ресурс как type/name, например string/app_name");
        }
        String[] parts = id.split("/");
        Pattern pattern = Pattern.compile(
                "(?<![A-Za-z0-9_])@(?:\\+)?" + Pattern.quote(parts[0]) + "/" + Pattern.quote(parts[1])
                        + "(?![A-Za-z0-9_])|R\\$" + Pattern.quote(parts[0]) + ";->" + Pattern.quote(parts[1])
                        + "(?=[:(])|R\\." + Pattern.quote(parts[0]) + "\\." + Pattern.quote(parts[1])
                        + "(?![A-Za-z0-9_])");
        File base = canonicalRoot(root);
        Report report = new Report();
        List<File> files = walk(base, report, listener, stop);
        List<File> selected = new ArrayList<>();
        for (File file : files) {
            cancel(stop);
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".xml") || name.endsWith(".smali") || name.endsWith(".java") || name.endsWith(".kt")) {
                selected.add(file);
            }
        }
        progress(listener, "Поиск использования", 0, selected.size(), "");
        int done = 0;
        for (File file : selected) {
            cancel(stop);
            String path = relative(base, file);
            if (!textAllowed(file)) {
                report.skipped++;
                report.add(path, 0, "Файл пропущен: размер больше 4 МБ");
            } else {
                boolean read = false;
                try (BufferedReader in = reader(file)) {
                    String line;
                    int number = 0;
                    while ((line = in.readLine()) != null) {
                        number++;
                        if (number % CANCEL_CHECK_LINES == 1) {
                            cancel(stop);
                        }
                        if (pattern.matcher(line).find()) {
                            String trimmed = line.trim();
                            report.add(path, number, trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed);
                        }
                    }
                    read = true;
                } catch (InterruptedIOException stopSignal) {
                    throw stopSignal;
                } catch (IOException error) {
                    report.add(path, 0, "Не удалось прочитать: " + error.getMessage());
                }
                if (read) {
                    report.checked++;
                } else {
                    report.skipped++;
                }
            }
            done++;
            progress(listener, "Поиск использования", done, selected.size(), path);
        }
        return report;
    }

    private static boolean isBinaryXml(byte[] bytes, String text) {
        return (bytes.length >= 4 && bytes[0] == 3 && bytes[1] == 0 && bytes[2] == 8 && bytes[3] == 0)
                || text.indexOf(0) >= 0;
    }

    private static boolean hasDtd(String text) {
        return text.contains("<!DOCTYPE");
    }

    private static void collectDefinitions(String text, Set<String> definitions) {
        Matcher declaration = RESOURCE_DECLARATION.matcher(text);
        while (declaration.find()) {
            String type = declaration.group(1);
            if (type.endsWith("-array")) {
                type = "array";
            }
            definitions.add(type + "/" + declaration.group(2));
        }
        Matcher typed = TYPED_ITEM.matcher(text);
        while (typed.find()) {
            Matcher attributes = ITEM_ATTRIBUTE.matcher(typed.group(1));
            String type = null;
            String name = null;
            while (attributes.find()) {
                if ("type".equals(attributes.group(1))) {
                    type = attributes.group(2);
                } else {
                    name = attributes.group(2);
                }
            }
            if (type != null && name != null) {
                definitions.add(type + "/" + name);
            }
        }
        Matcher ids = PLUS_ID.matcher(text);
        while (ids.find()) {
            definitions.add("id/" + ids.group(1));
        }
    }

    private static Set<String> resourceIndex(File root, List<File> files, Report report, Progress listener,
                                             BooleanSupplier cancelled) throws IOException {
        Set<String> definitions = new HashSet<>();
        int done = 0;
        progress(listener, "Поиск ресурсов", 0, files.size(), "");
        for (File file : files) {
            cancel(cancelled);
            String path = relative(root, file);
            if (path.startsWith("res/")) {
                String[] parts = path.split("/");
                if (parts.length == 3 && !parts[1].startsWith("values")) {
                    String type = parts[1].split("-")[0];
                    String name = parts[2];
                    int dot = name.indexOf('.');
                    if (dot > 0) {
                        definitions.add(type + "/" + name.substring(0, dot));
                    }
                }
            }
            if (path.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                boolean resourceXml = path.startsWith("res/");
                if (!textAllowed(file)) {
                    if (resourceXml) {
                        report.resourceIndexIncomplete = true;
                    }
                } else {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        cancel(cancelled);
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        if (isBinaryXml(bytes, text) || hasDtd(text)) {
                            if (resourceXml) {
                                report.resourceIndexIncomplete = true;
                            }
                        } else {
                            collectDefinitions(text, definitions);
                        }
                    } catch (IOException error) {
                        if (resourceXml) {
                            report.resourceIndexIncomplete = true;
                        }
                    }
                }
            }
            done++;
            progress(listener, "Поиск ресурсов", done, files.size(), path);
        }
        return definitions;
    }

    private static void setFeature(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // The parser still blocks external entities through its EntityResolver below.
        }
    }

    private static boolean checkXml(File root, File file, Report report, Set<String> definitions,
                                    BooleanSupplier cancelled) throws InterruptedIOException {
        String path = relative(root, file);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException error) {
            report.add(path, 0, "Не удалось прочитать: " + error.getMessage());
            return false;
        }
        cancel(cancelled);
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (isBinaryXml(bytes, text)) {
            report.add(path, 0, "XML не проверен: бинарная кодировка");
            return false;
        }
        if (hasDtd(text)) {
            report.add(path, 0, "XML не проверен: DTD не поддерживается");
            return false;
        }
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            XMLReader xml = factory.newSAXParser().getXMLReader();
            xml.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            xml.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException error) throws SAXException {
                    throw error;
                }

                @Override
                public void fatalError(SAXParseException error) throws SAXException {
                    throw error;
                }
            });
            xml.parse(new InputSource(new ByteArrayInputStream(bytes)));
        } catch (SAXParseException error) {
            report.add(path, error.getLineNumber(), "XML: " + error.getMessage());
        } catch (ParserConfigurationException | SAXException error) {
            report.add(path, 0, "Не удалось проверить XML: " + error.getMessage());
        } catch (IOException error) {
            report.add(path, 0, "Не удалось проверить XML: " + error.getMessage());
            return false;
        }
        cancel(cancelled);
        try (BufferedReader lines = new BufferedReader(new StringReader(text))) {
            String line;
            int number = 0;
            while ((line = lines.readLine()) != null) {
                number++;
                if (number % CANCEL_CHECK_LINES == 1) {
                    cancel(cancelled);
                }
                Matcher references = RESOURCE_REFERENCE.matcher(line);
                while (references.find()) {
                    String key = references.group(1) + "/" + references.group(2);
                    if (!definitions.contains(key)) {
                        report.add(path, number, "Возможно отсутствует ресурс @" + key + " (проверка приблизительная)");
                    }
                }
            }
        } catch (IOException error) {
            report.add(path, 0, "Не удалось прочитать XML: " + error.getMessage());
        }
        return true;
    }

    private static boolean isEndMethod(String trimmed) {
        return trimmed.equals(".end method") || trimmed.startsWith(".end method ") || trimmed.startsWith(".end method\t");
    }

    private static boolean checkSmaliStructure(File root, File file, Report report, BooleanSupplier cancelled)
            throws InterruptedIOException {
        String path = relative(root, file);
        boolean hasClass = false;
        boolean inMethod = false;
        int methodLine = 0;
        try (BufferedReader in = reader(file)) {
            String line;
            int number = 0;
            while ((line = in.readLine()) != null) {
                number++;
                if (number % CANCEL_CHECK_LINES == 1) {
                    cancel(cancelled);
                }
                String trimmed = line.trim();
                if (trimmed.startsWith(".class ")) {
                    hasClass = true;
                }
                if (trimmed.startsWith(".method ")) {
                    if (inMethod) {
                        report.add(path, number, "Вложенное .method; проверьте .end method");
                    }
                    methodLine = number;
                    inMethod = true;
                } else if (isEndMethod(trimmed)) {
                    if (!inMethod) {
                        report.add(path, number, ".end method без .method");
                    }
                    inMethod = false;
                }
            }
        } catch (InterruptedIOException stop) {
            throw stop;
        } catch (IOException error) {
            report.add(path, 0, "Не удалось прочитать: " + error.getMessage());
            return false;
        }
        if (inMethod) {
            report.add(path, methodLine, "Метод не закрыт .end method");
        }
        if (!hasClass) {
            report.add(path, 1, "Не найдена директива .class");
        }
        return true;
    }

    static int smaliApiFor(File root, File file) {
        File parent = file.getParentFile();
        while (parent != null && parent.toPath().startsWith(root.toPath())) {
            File marker = new File(parent, ".apkeditor-dex-api");
            if (marker.isFile() && !Files.isSymbolicLink(marker.toPath())) {
                try (BufferedReader in = reader(marker)) {
                    String line = in.readLine();
                    if (line != null) {
                        int api = Integer.parseInt(line.trim());
                        if (api >= 1 && api <= MAX_SMALI_API) {
                            return api;
                        }
                    }
                } catch (IOException | NumberFormatException ignored) {
                    // Fall back to the default API for a missing or malformed marker.
                }
                return DEFAULT_SMALI_API;
            }
            parent = parent.getParentFile();
        }
        return DEFAULT_SMALI_API;
    }

    private static void addSmaliIssues(Report report, String path, String[] issues) {
        if (issues == null) {
            return;
        }
        for (String issue : issues) {
            if (issue == null) {
                continue;
            }
            int line = 0;
            String message = issue;
            int colon = issue.indexOf(':');
            if (colon > 0) {
                String prefix = issue.substring(0, colon);
                boolean digits = !prefix.isEmpty();
                for (int i = 0; i < prefix.length(); i++) {
                    if (!Character.isDigit(prefix.charAt(i))) {
                        digits = false;
                        break;
                    }
                }
                if (digits) {
                    try {
                        line = Integer.parseInt(prefix);
                        message = issue.substring(colon + 1);
                    } catch (NumberFormatException ignored) {
                        line = 0;
                        message = issue;
                    }
                }
            }
            report.add(path, line, "smali: " + message);
        }
    }

    public static Report diagnose(File root) throws Exception {
        return diagnose(root, null, NEVER);
    }

    public static Report diagnose(File root, Progress listener, BooleanSupplier cancelled) throws Exception {
        BooleanSupplier stop = cancelled == null ? NEVER : cancelled;
        File base = canonicalRoot(root);
        Report report = new Report();
        List<File> files = walk(base, report, listener, stop);
        if (report.truncated || report.skipped > 0) {
            report.resourceIndexIncomplete = true;
        }
        Set<String> definitions = resourceIndex(base, files, report, listener, stop);
        List<File> selected = new ArrayList<>();
        for (File file : files) {
            cancel(stop);
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".xml")) {
                selected.add(file);
                report.xmlFound++;
            } else if (name.endsWith(".smali")) {
                selected.add(file);
                report.smaliFound++;
            }
        }
        Method smaliChecker = null;
        if (report.smaliFound > 0) {
            try {
                smaliChecker = Class.forName("com.gmail.heagoo.apkeditor.pro.SmaliCheck")
                        .getMethod("check", String.class, Integer.TYPE);
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError unavailable) {
                report.smaliSyntaxUnavailable = true;
            }
        }
        Map<File, Integer> apiCache = new HashMap<>();
        int done = 0;
        progress(listener, "Проверка XML и smali", 0, selected.size(), "");
        for (File file : selected) {
            cancel(stop);
            String path = relative(base, file);
            String name = file.getName().toLowerCase(Locale.ROOT);
            boolean checked = false;
            if (!textAllowed(file)) {
                report.skipped++;
                report.add(path, 0, "Файл пропущен: размер больше 4 МБ");
            } else if (name.endsWith(".xml")) {
                checked = checkXml(base, file, report, definitions, stop);
                if (checked) {
                    report.xmlChecked++;
                } else {
                    report.skipped++;
                }
            } else {
                checked = checkSmaliStructure(base, file, report, stop);
                if (checked) {
                    report.smaliChecked++;
                    if (smaliChecker == null) {
                        report.smaliNotParsed++;
                    } else {
                        File parent = file.getParentFile();
                        Integer api = apiCache.get(parent);
                        if (api == null) {
                            api = smaliApiFor(base, file);
                            apiCache.put(parent, api);
                        }
                        cancel(stop);
                        try {
                            String[] issues = (String[]) smaliChecker.invoke(null, file.getAbsolutePath(), api);
                            addSmaliIssues(report, path, issues);
                        } catch (InvocationTargetException error) {
                            Throwable cause = error.getCause() == null ? error : error.getCause();
                            if (cause instanceof InterruptedIOException) {
                                throw (InterruptedIOException) cause;
                            }
                            if (cause instanceof OutOfMemoryError) {
                                throw (OutOfMemoryError) cause;
                            }
                            if (cause instanceof LinkageError) {
                                report.smaliSyntaxUnavailable = true;
                                smaliChecker = null;
                                report.smaliNotParsed++;
                            } else {
                                report.smaliNotParsed++;
                                report.add(path, 0, "Не удалось проверить smali: " + cause);
                            }
                        } catch (IllegalAccessException | LinkageError unavailable) {
                            report.smaliSyntaxUnavailable = true;
                            smaliChecker = null;
                            report.smaliNotParsed++;
                        }
                    }
                } else {
                    report.skipped++;
                }
            }
            if (checked) {
                report.checked++;
            }
            done++;
            progress(listener, "Проверка XML и smali", done, selected.size(), path);
        }
        return report;
    }
}
