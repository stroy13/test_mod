package com.gmail.heagoo.apkeditor.patch;

import android.content.Context;
import com.gmail.heagoo.folderlist.FileSort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: /tmp/apkx/classes7.dex */
public final class PatchPreflight {

    public static final class Report {
        public final String details;
        public final String summary;

        Report(String summary, String details) {
            this.summary = summary;
            this.details = details;
        }
    }

    private static class Audit implements IPatchContext {
        final IPatchContext base;
        final Context context;
        int errors;
        final StringBuilder out = new StringBuilder();
        final Map<String, String> vars = new HashMap();
        final List<String> names = new ArrayList();

        Audit(Context c, IPatchContext b) {
            this.context = c;
            this.base = b;
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public String getDecodeRootPath() {
            return this.base.getDecodeRootPath();
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public List<String> getSmaliFolders() {
            return this.base.getSmaliFolders();
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public List<String> getActivities() {
            return this.base.getActivities();
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public String getApplication() {
            return this.base.getApplication();
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public List<String> getLauncherActivities() {
            return this.base.getLauncherActivities();
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public List<String> getPatchNames() {
            return this.names;
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void info(int id, boolean bold, Object... args) {
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void info(String text, boolean bold, Object... args) {
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void fileEntry(String text) {
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void error(int id, Object... args) {
            this.errors++;
            if (this.out.length() > 100000) {
                return;
            }
            try {
                this.out.append(this.context.getString(id, args));
            } catch (Exception e) {
                this.out.append(this.context.getString(id)).append(" ").append(Arrays.toString(args));
            }
            this.out.append('\n');
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public String getString(int id) {
            return this.context.getString(id);
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public String getVariableValue(String k) {
            return this.vars.get(k);
        }

        public boolean hasErrors() {
            return this.errors > 0;
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void patchFinished() {
        }

        @Override // com.gmail.heagoo.apkeditor.patch.IPatchContext
        public void setVariableValue(String k, String v) {
            this.vars.put(k, v);
        }
    }

    private static Object field(Object object, String name) {
        try {
            Field f = object.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(object);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] limited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        while (true) {
            int n = in.read(b);
            if (n == -1) {
                return out.toByteArray();
            }
            if (out.size() + n > 1048576) {
                throw new IOException("Файл patch.txt больше 1 МБ");
            }
            out.write(b, 0, n);
        }
    }

    private static String value(Object v) {
        if (v == null || v.toString().trim().isEmpty()) {
            return "не указан";
        }
        String s = v.toString();
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }

    private static String label(String type) {
        char c;
        switch (type.hashCode()) {
            case -1785583463:
                if (type.equals("ReviseSig")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1698419377:
                if (type.equals("MatchReplace")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1677359960:
                if (type.equals("MatchGoto")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1498298636:
                if (type.equals("MatchAssign")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1194593674:
                if (type.equals("AddFiles")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -502600912:
                if (type.equals("FuncReplace")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 2225539:
                if (type.equals("Goto")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 66393224:
                if (type.equals("Dummy")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 74232856:
                if (type.equals("Merge")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 345036198:
                if (type.equals("ExecDex")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 1350904883:
                if (type.equals("RemoveFiles")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case FileSort.NAME /* 0 */:
                return "Добавление файлов";
            case 1:
                return "Удаление файлов";
            case FileSort.SIZE /* 2 */:
                return "Объединение ресурсов и файлов";
            case FileSort.TYPE /* 3 */:
                return "Поиск и замена";
            case 4:
                return "Переход по условию";
            case 5:
                return "Поиск и создание переменной";
            case 6:
                return "Переход к другому шагу";
            case 7:
                return "Служебная отметка";
            case '\b':
                return "Замена функции";
            case '\t':
                return "Изменение данных подписи";
            case '\n':
                return "Выполнение встроенного кода";
            default:
                return "Другие действия (" + type + ")";
        }
    }

    public static String inspect(Context context, IPatchContext base, File file) throws Exception {
        Report r = inspectReport(context, base, file);
        return r.summary + "\n\n" + r.details;
    }

    public static Report inspectReport(Context context, IPatchContext base, File file) throws Exception {
        ZipFile zip = null;
        Throwable th = null;
        ZipEntry text = null;
        int count = 0;
        int invalid = 0;
        Patch patch = null;
        Iterator<PatchRule> it = null;
        int intValue = 0;
        PatchRule rule = null;
        boolean merge = false;
        Object target = null;
        Object target2 = null;
        int missing = 0;
        Set<String> seenTargets = null;
        Iterator<PatchRule> it2;
        int count2;
        Audit audit = new Audit(context, base);
        StringBuilder details = new StringBuilder();
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        Set<String> seenTargets2 = new HashSet<>();
        int invalid2 = 0;
        int missing2 = 0;
        int existing = 0;
        int repeated = 0;
        int suspicious = 0;
        boolean dex = false;
        boolean merge2 = false;
        boolean control = false;
        ZipFile zip2 = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> e = zip2.entries();
            text = null;
            count = 0;
            while (e.hasMoreElements()) {
                try {
                    ZipEntry x = e.nextElement();
                    int invalid3 = invalid2;
                    try {
                        if ("patch.txt".equals(x.getName())) {
                            count++;
                            text = x;
                        }
                        invalid2 = invalid3;
                    } catch (Throwable th2) {
                        th = th2;
                        zip = zip2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    zip = zip2;
                }
            }
            invalid = invalid2;
            try {
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Throwable th5) {
            zip = zip2;
            th = th5;
        }
        if (count != 1) {
            throw new IOException("В архиве должен находиться один файл patch.txt в корне.");
        }
        ZipFile zip3 = zip2;
        try {
            InputStream in = zip3.getInputStream(text);
            try {
                byte[] bytes = limited(in);
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable th6) {
                        th = th6;
                        zip = zip3;
                    }
                }
                patch = PatchParser.parse(new ByteArrayInputStream(bytes), audit);
                Iterator<PatchRule> it3 = patch.rules.iterator();
                while (it3.hasNext()) {
                    PatchRule rule2 = it3.next();
                    if (rule2.getRuleName() != null) {
                        it2 = it3;
                        count2 = count;
                        audit.names.add(rule2.getRuleName());
                    } else {
                        it2 = it3;
                        count2 = count;
                    }
                    it3 = it2;
                    count = count2;
                }
                it = patch.rules.iterator();
            } catch (Throwable th7) {
                zip = zip3;
                if (in == null) {
                    throw th7;
                }
                try {
                    in.close();
                    throw th7;
                } catch (Throwable th8) {
                    th7.addSuppressed(th8);
                    throw th7;
                }
            }
        } catch (Throwable th9) {
            zip = zip3;
            th = th9;
        }
        while (true) {
            try {
                Iterator<PatchRule> it4 = it;
                if (!it.hasNext()) {
                    break;
                }
                PatchRule rule3 = it4.next();
                boolean dex2 = dex;
                try {
                    String type = rule3.getClass().getSimpleName().replace("PatchRule_", "");
                    String human = label(type);
                    if (counts.containsKey(human)) {
                        try {
                            intValue = counts.get(human).intValue() + 1;
                        } catch (Throwable th10) {
                            th = th10;
                            zip = zip3;
                        }
                    } else {
                        intValue = 1;
                    }
                    counts.put(human, Integer.valueOf(intValue));
                    boolean dex3 = dex2 | type.equals("ExecDex");
                    boolean merge3 = merge2 | type.equals("Merge");
                    try {
                        control |= type.equals("Goto") || type.equals("MatchGoto");
                        Map<String, Integer> counts2 = counts;
                        if (details.length() < 180000) {
                            try {
                                rule = rule3;
                                merge = merge3;
                                try {
                                    details.append("Строка ").append(rule.startLine).append(" · ").append(human);
                                    if (rule.getRuleName() != null) {
                                        details.append(" — ").append(value(rule.getRuleName()));
                                    }
                                    details.append('\n');
                                } catch (Throwable th11) {
                                    th = th11;
                                    zip = zip3;
                                }
                            } catch (Throwable th12) {
                                th = th12;
                                zip = zip3;
                            }
                        } else {
                            rule = rule3;
                            merge = merge3;
                        }
                        try {
                            Object source = field(rule, "sourceFile");
                            Object source2 = source == null ? field(rule, "scriptName") : source;
                            if (source2 instanceof String) {
                                String path = (String) source2;
                                if (details.length() < 180000) {
                                    details.append("  В архиве: ").append(value(path)).append('\n');
                                }
                                if (zip3.getEntry(path) == null) {
                                    missing2++;
                                    if (details.length() < 180000) {
                                        details.append("  Не найден в архиве; проверьте путь и содержимое ZIP.\n");
                                    }
                                }
                            }
                            try {
                                Object target3 = field(rule, "targetFile");
                                Object targets = field(rule, "targetList");
                                Object finder = field(rule, "pathFinder");
                                try {
                                    if (target3 == null) {
                                        target = target3;
                                        if (finder instanceof PathFinder) {
                                            zip = zip3;
                                            target2 = ((PathFinder) finder).getOriginalTarget();
                                            if (target2 == null) {
                                                try {
                                                    missing = missing2;
                                                    if (details.length() < 180000) {
                                                        try {
                                                            details.append("  Куда: ").append(value(target2)).append('\n');
                                                        } catch (Throwable th13) {
                                                            th = th13;
                                                        }
                                                    }
                                                } catch (Throwable th14) {
                                                    th = th14;
                                                }
                                            } else {
                                                missing = missing2;
                                            }
                                            if (targets != null && details.length() < 180000) {
                                                details.append("  Файлы: ").append(value(targets)).append('\n');
                                            }
                                            if (target2 instanceof String) {
                                                seenTargets = seenTargets2;
                                            } else {
                                                String t = (String) target2;
                                                if (seenTargets2.add(t)) {
                                                    seenTargets = seenTargets2;
                                                } else {
                                                    repeated++;
                                                    seenTargets = seenTargets2;
                                                    if (details.length() < 180000) {
                                                        try {
                                                            details.append("  Путь встречается ещё раз; это не обязательно конфликт.\n");
                                                        } catch (Throwable th15) {
                                                            th = th15;
                                                        }
                                                    }
                                                }
                                                if (t.equals("..") || t.contains("../") || t.contains("..\\")) {
                                                    suspicious++;
                                                    if (details.length() < 180000) {
                                                        details.append("  Подозрительный путь — проверьте его перед применением.\n");
                                                    }
                                                } else if (base.getDecodeRootPath() != null && type.equals("AddFiles") && new File(base.getDecodeRootPath(), t).exists()) {
                                                    existing++;
                                                    if (details.length() < 180000) {
                                                        details.append("  Файл уже есть в проекте; возможна замена или конфликт.\n");
                                                    }
                                                }
                                            }
                                            if (!rule.isValid(audit)) {
                                                int invalid4 = invalid + 1;
                                                try {
                                                    try {
                                                        if (details.length() < 180000) {
                                                            details.append("  Не пройдена проверка обязательных полей или действие не поддерживается.\n");
                                                        }
                                                        invalid = invalid4;
                                                    } catch (Exception e2) {
                                                        Exception ex = e2;
                                                        invalid = invalid4;
                                                        int invalid5 = invalid + 1;
                                                        if (details.length() < 180000) {
                                                            details.append("  Проверить правило не удалось: ").append(value(ex)).append('\n');
                                                        }
                                                        invalid = invalid5;
                                                        if (details.length() < 180000) {
                                                        }
                                                        it = it4;
                                                        merge2 = merge;
                                                        dex = dex3;
                                                        counts = counts2;
                                                        missing2 = missing;
                                                        zip3 = zip;
                                                        seenTargets2 = seenTargets;
                                                    }
                                                } catch (Throwable th16) {
                                                    th = th16;
                                                }
                                            }
                                            if (details.length() < 180000) {
                                                details.append('\n');
                                            }
                                            it = it4;
                                            merge2 = merge;
                                            dex = dex3;
                                            counts = counts2;
                                            missing2 = missing;
                                            zip3 = zip;
                                            seenTargets2 = seenTargets;
                                        }
                                    } else {
                                        target = target3;
                                    }
                                    if (target2 instanceof String) {
                                    }
                                    if (!rule.isValid(audit)) {
                                    }
                                    if (details.length() < 180000) {
                                    }
                                    it = it4;
                                    merge2 = merge;
                                    dex = dex3;
                                    counts = counts2;
                                    missing2 = missing;
                                    zip3 = zip;
                                    seenTargets2 = seenTargets;
                                } catch (Throwable th17) {
                                    th = th17;
                                }
                                zip = zip3;
                                target2 = target;
                                if (target2 == null) {
                                }
                                if (targets != null) {
                                    details.append("  Файлы: ").append(value(targets)).append('\n');
                                }
                            } catch (Throwable th18) {
                                zip = zip3;
                                th = th18;
                            }
                        } catch (Throwable th19) {
                            zip = zip3;
                            th = th19;
                        }
                    } catch (Throwable th20) {
                        zip = zip3;
                        th = th20;
                    }
                } catch (Throwable th21) {
                    zip = zip3;
                    th = th21;
                }
            } catch (Throwable th22) {
                zip = zip3;
                th = th22;
            }
            try {
                zip.close();
                throw (th instanceof Exception) ? (Exception) th : new Exception(th);
            } catch (Throwable th23) {
                th.addSuppressed(th23);
                throw (th instanceof Exception) ? (Exception) th : new Exception(th);
            }
        }
        Map<String, Integer> counts3 = counts;
        zip = zip3;
        boolean dex4 = dex;
        try {
            StringBuilder summary = new StringBuilder();
            if (audit.errors > 0 || invalid > 0) {
                summary.append("Найдены ошибки в описании или обязательных полях. Проверьте подробности перед применением.\n\n");
            } else if (missing2 > 0) {
                summary.append("В архиве не найдены некоторые необходимые файлы. Проверьте замечания перед применением.\n\n");
            } else {
                summary.append("Ошибок в проверенных полях не найдено. Это ещё не означает, что патч успешно применится.\n\n");
            }
            summary.append("Патч: ").append(value(file.getName())).append("\nАвтор: ").append(value(patch.author)).append("\nДля пакета: ").append(value(patch.packagename)).append("\nДействий: ").append(patch.rules.size()).append("\n");
            if (patch.requiredEngine > 0) {
                summary.append("Минимальная версия движка: ").append(patch.requiredEngine).append('\n');
            }
            if (!counts3.isEmpty()) {
                summary.append("\nЧто патч пытается сделать:\n");
                for (Map.Entry<String, Integer> c : counts3.entrySet()) {
                    summary.append("• ").append(c.getKey()).append(": ").append(c.getValue()).append('\n');
                }
            }
            if (patch.requiredEngine > 1) {
                issues.add("Патчу нужна более новая версия движка; выполнение может быть невозможно.");
            }
            if (suspicious > 0) {
                issues.add("Обнаружены подозрительные пути: " + suspicious + ". Проверьте их в подробностях.");
            }
            if (missing2 > 0) {
                issues.add("Не найдены файлы, указанные в архиве: " + missing2 + ". Откройте подробности и проверьте ZIP.");
            }
            if (existing > 0) {
                issues.add("В проекте уже есть файлов по указанным путям: " + existing + ". При добавлении возможна замена или конфликт.");
            }
            if (repeated > 0) {
                issues.add("Один путь указан несколько раз: " + repeated + ". Это не обязательно ошибка, но стоит проверить порядок действий.");
            }
            if (dex4) {
                issues.add("Патч содержит исполняемый DEX-код. Его действия и последствия заранее не проверяются; используйте патчи только из доверенных источников.");
            }
            if (merge2) {
                issues.add("Итог объединения файлов и ресурсов станет известен только при применении патча.");
            }
            if (control) {
                issues.add("Часть шагов выполняется по условиям; заранее определить весь порядок нельзя.");
            }
            if (!issues.isEmpty()) {
                summary.append("\nНа что обратить внимание:\n");
                for (String issue : issues) {
                    summary.append("• ").append(issue).append('\n');
                }
            }
            summary.append("\nКак проводилась проверка: прочитаны описание и поля правил. Патч не запускался и файлы проекта не изменялись. Это не проверка результата после применения.");
            if (audit.errors > 0) {
                details.append("Ошибки чтения и проверки полей:\n").append((CharSequence) audit.out);
            }
            if (details.length() >= 180000) {
                details.append("\nПодробности сокращены.\n");
            }
            Report report = new Report(summary.toString(), details.toString());
            zip.close();
            return report;
        } catch (Throwable th24) {
            th = th24;
        }
        try {
            zip.close();
        } catch (Throwable ignored) {
        }
        throw (th instanceof Exception) ? (Exception) th : new Exception(th);
    }

    private PatchPreflight() {
    }
}
