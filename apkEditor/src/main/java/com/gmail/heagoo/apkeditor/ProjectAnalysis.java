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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    public enum FindingKind {
        ERROR,
        WARNING,
        NOT_CHECKED,
        MATCH;

        static FindingKind forMessage(String message) {
            if (message == null) {
                return ERROR;
            }
            if (message.startsWith("Возможно отсутствует ресурс")) {
                return WARNING;
            }
            if (message.startsWith("Файл пропущен") || message.startsWith("XML не проверен")
                    || message.startsWith("Не удалось прочитать") || message.startsWith("Не удалось проверить XML")
                    || message.startsWith("Не удалось проверить smali")) {
                return NOT_CHECKED;
            }
            return ERROR;
        }
    }

    public static final class Finding {
        public final int line;
        public final String message;
        public final String path;
        public final FindingKind kind;

        Finding(String path, int line, String message) {
            this(path, line, message, FindingKind.forMessage(message));
        }

        Finding(String path, int line, String message, FindingKind kind) {
            this.path = path;
            this.line = line;
            this.message = message;
            this.kind = kind == null ? FindingKind.forMessage(message) : kind;
        }

        @Override
        public String toString() {
            return this.path + (this.line > 0 ? ":" + this.line : "") + " — " + this.message;
        }
    }

    private static final Comparator<Finding> FINDING_ORDER = Comparator
            .comparingInt((Finding finding) -> finding.kind.ordinal())
            .thenComparing(finding -> finding.path)
            .thenComparingInt(finding -> finding.line)
            .thenComparing(finding -> finding.message);

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
        public boolean fileLimitReached;
        public boolean findingsTruncated;
        public int totalFindings;
        public int errors;
        public int warnings;
        public int notChecked;
        public int matches;
        public int xmlChecked;

        synchronized void add(String path, int line, String message) {
            add(path, line, message, FindingKind.forMessage(message));
        }

        synchronized void add(String path, int line, String message, FindingKind kind) {
            Finding finding = new Finding(path, line, message, kind);
            this.totalFindings++;
            switch (finding.kind) {
                case ERROR:
                    this.errors++;
                    break;
                case WARNING:
                    this.warnings++;
                    break;
                case NOT_CHECKED:
                    this.notChecked++;
                    break;
                case MATCH:
                    this.matches++;
                    break;
            }
            int index = Collections.binarySearch(this.findings, finding, FINDING_ORDER);
            if (index < 0) {
                index = -index - 1;
            }
            if (this.findings.size() < MAX_FINDINGS) {
                this.findings.add(index, finding);
            } else {
                this.findingsTruncated = true;
                this.truncated = true;
                if (FINDING_ORDER.compare(finding, this.findings.get(this.findings.size() - 1)) < 0) {
                    this.findings.remove(this.findings.size() - 1);
                    this.findings.add(index, finding);
                }
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

    private static final Object PROGRESS_LOCK = new Object();

    private static void progress(Progress listener, String stage, int done, int total, String path) {
        if (listener != null) {
            synchronized (PROGRESS_LOCK) {
                listener.update(stage, done, total, path);
            }
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
                        report.fileLimitReached = true;
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
                            report.add(path, number, trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed,
                                    FindingKind.MATCH);
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

    private static Set<String> resourceIndex(File root, List<File> resourceFiles, Report report, Progress listener,
                                             BooleanSupplier cancelled) throws IOException {
        Set<String> definitions = new HashSet<>();
        int done = 0;
        progress(listener, "Поиск ресурсов", 0, resourceFiles.size(), "");
        for (File file : resourceFiles) {
            cancel(cancelled);
            String path = relative(root, file);
            String[] parts = path.split("/");
            if (parts.length == 3 && !parts[1].startsWith("values")) {
                String type = parts[1].split("-")[0];
                String name = parts[2];
                int dot = name.indexOf('.');
                if (dot > 0) {
                    definitions.add(type + "/" + name.substring(0, dot));
                }
            }
            if (path.toLowerCase(Locale.ROOT).endsWith(".xml") && checksResourceReferences(path)) {
                if (!textAllowed(file)) {
                    report.resourceIndexIncomplete = true;
                } else {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        cancel(cancelled);
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        if (isBinaryXml(bytes, text) || hasDtd(text)) {
                            report.resourceIndexIncomplete = true;
                        } else {
                            collectDefinitions(text, definitions);
                        }
                    } catch (IOException error) {
                        report.resourceIndexIncomplete = true;
                    }
                }
            }
            done++;
            if (done % 32 == 0 || done == resourceFiles.size()) {
                progress(listener, "Поиск ресурсов", done, resourceFiles.size(), path);
            }
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

    private static SAXParserFactory newXmlFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory;
    }

    private static final ThreadLocal<SAXParserFactory> XML_FACTORY = new ThreadLocal<SAXParserFactory>() {
        @Override
        protected SAXParserFactory initialValue() {
            return newXmlFactory();
        }
    };

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
            XMLReader xml = XML_FACTORY.get().newSAXParser().getXMLReader();
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
        // Only compiled Android resource XML resolves @type/name references. Files in res/raw
        // (for example Firebase shrinker keep rules) and assets are arbitrary payloads: an @string
        // there is not a missing Android resource and must not be reported as one.
        if (checksResourceReferences(path)) {
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
                            report.add(path, number, "Возможно отсутствует ресурс @" + key + " (проверка приблизительная)",
                                    FindingKind.WARNING);
                        }
                    }
                }
            } catch (IOException error) {
                report.add(path, 0, "Не удалось прочитать XML: " + error.getMessage(), FindingKind.NOT_CHECKED);
            }
        }
        return true;
    }

    static boolean checksResourceReferences(String path) {
        if (path.equals("AndroidManifest.xml")) {
            return true;
        }
        if (!path.startsWith("res/")) {
            return false;
        }
        int slash = path.indexOf('/', 4);
        if (slash < 0) {
            return false;
        }
        String directory = path.substring(4, slash);
        return !directory.equals("raw") && !directory.startsWith("raw-");
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

    static int workerCount() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        long memory = Runtime.getRuntime().maxMemory();
        int byMemory = memory <= 0 ? 4 : Math.max(1, (int) (memory / (128L * 1024L * 1024L)));
        return Math.max(1, Math.min(4, Math.min(cores, byMemory)));
    }

    private static final class FileOutcome {
        boolean checked;
        int xmlChecked;
        int smaliChecked;
        int skipped;
        int smaliNotParsed;
        boolean syntaxUnavailable;
    }

    private static FileOutcome checkOne(File root, File file, Report report, Set<String> definitions,
                                        Method smaliChecker, boolean checkerUsesSupplier,
                                        AtomicBoolean parserAvailable, Map<File, Integer> apiCache,
                                        BooleanSupplier cancelled) throws InterruptedIOException {
        cancel(cancelled);
        FileOutcome outcome = new FileOutcome();
        String path = relative(root, file);
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (!textAllowed(file)) {
            outcome.skipped = 1;
            report.add(path, 0, "Файл пропущен: размер больше 4 МБ");
            return outcome;
        }
        if (name.endsWith(".xml")) {
            if (checkXml(root, file, report, definitions, cancelled)) {
                outcome.checked = true;
                outcome.xmlChecked = 1;
            } else {
                outcome.skipped = 1;
            }
            return outcome;
        }

        if (smaliChecker != null && parserAvailable.get()) {
            Integer api = apiCache.get(file.getParentFile());
            if (api == null) {
                api = DEFAULT_SMALI_API;
            }
            cancel(cancelled);
            try {
                String[] issues = checkerUsesSupplier
                        ? (String[]) smaliChecker.invoke(null, file.getAbsolutePath(), api, cancelled)
                        : (String[]) smaliChecker.invoke(null, file.getAbsolutePath(), api);
                addSmaliIssues(report, path, issues);
                outcome.checked = true;
                outcome.smaliChecked = 1;
                return outcome;
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause() == null ? error : error.getCause();
                if (cause instanceof InterruptedIOException) {
                    throw (InterruptedIOException) cause;
                }
                if (cause instanceof OutOfMemoryError) {
                    throw (OutOfMemoryError) cause;
                }
                if (cause instanceof LinkageError) {
                    parserAvailable.set(false);
                    outcome.syntaxUnavailable = true;
                } else {
                    outcome.smaliNotParsed = 1;
                    report.add(path, 0, "Не удалось проверить smali: " + cause);
                }
            } catch (IllegalAccessException | LinkageError unavailable) {
                parserAvailable.set(false);
                outcome.syntaxUnavailable = true;
            }
        }

        if (checkSmaliStructure(root, file, report, cancelled)) {
            outcome.checked = true;
            outcome.smaliChecked = 1;
            outcome.smaliNotParsed = 1;
        } else {
            outcome.skipped = 1;
            outcome.smaliNotParsed = 0;
        }
        return outcome;
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
        List<File> selected = new ArrayList<>();
        List<File> resourceFiles = new ArrayList<>();
        Map<File, Integer> apiCache = new HashMap<>();
        for (File file : files) {
            cancel(stop);
            String path = relative(base, file);
            if (path.startsWith("res/")) {
                resourceFiles.add(file);
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".xml")) {
                selected.add(file);
                report.xmlFound++;
            } else if (name.endsWith(".smali")) {
                selected.add(file);
                report.smaliFound++;
                File parent = file.getParentFile();
                if (!apiCache.containsKey(parent)) {
                    apiCache.put(parent, smaliApiFor(base, file));
                }
            }
        }
        Set<String> definitions = resourceIndex(base, resourceFiles, report, listener, stop);
        Method smaliChecker = null;
        boolean checkerUsesSupplier = false;
        if (report.smaliFound > 0) {
            try {
                smaliChecker = Class.forName("com.gmail.heagoo.apkeditor.pro.SmaliCheck")
                        .getMethod("check", String.class, Integer.TYPE, BooleanSupplier.class);
                checkerUsesSupplier = true;
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError unavailable) {
                try {
                    smaliChecker = Class.forName("com.gmail.heagoo.apkeditor.pro.SmaliCheck")
                            .getMethod("check", String.class, Integer.TYPE);
                } catch (ClassNotFoundException | NoSuchMethodException | LinkageError missing) {
                    report.smaliSyntaxUnavailable = true;
                }
            }
        }
        int total = selected.size();
        progress(listener, "Проверка XML и smali", 0, total, "");
        if (total == 0) {
            return report;
        }
        int workers = Math.min(workerCount(), total);
        ExecutorService pool = Executors.newFixedThreadPool(workers, new java.util.concurrent.ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "project-analysis-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        AtomicInteger next = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();
        FileOutcome[] outcomes = new FileOutcome[total];
        AtomicBoolean parserAvailable = new AtomicBoolean(smaliChecker != null);
        final Method checker = smaliChecker;
        final boolean usesSupplier = checkerUsesSupplier;
        CompletionService<Void> completion = new ExecutorCompletionService<>(pool);
        try {
            for (int i = 0; i < workers; i++) {
                completion.submit(() -> {
                    while (true) {
                        cancel(stop);
                        int index = next.getAndIncrement();
                        if (index >= total) {
                            return null;
                        }
                        File file = selected.get(index);
                        outcomes[index] = checkOne(base, file, report, definitions, checker, usesSupplier,
                                parserAvailable, apiCache, stop);
                        synchronized (PROGRESS_LOCK) {
                            int current = done.incrementAndGet();
                            progress(listener, "Проверка XML и smali", current, total, relative(base, file));
                        }
                    }
                });
            }
            for (int i = 0; i < workers; i++) {
                try {
                    Future<Void> future = completion.take();
                    future.get();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Проверка отменена");
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause();
                    if (cause instanceof InterruptedIOException) {
                        throw (InterruptedIOException) cause;
                    }
                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    }
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw new IOException(cause);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        for (FileOutcome outcome : outcomes) {
            if (outcome == null) {
                continue;
            }
            if (outcome.checked) {
                report.checked++;
            }
            report.xmlChecked += outcome.xmlChecked;
            report.smaliChecked += outcome.smaliChecked;
            report.skipped += outcome.skipped;
            report.smaliNotParsed += outcome.smaliNotParsed;
            if (outcome.syntaxUnavailable) {
                report.smaliSyntaxUnavailable = true;
            }
        }
        report.findings.sort(FINDING_ORDER);
        return report;
    }
}
