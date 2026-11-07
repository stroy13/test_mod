package com.gmail.heagoo.apkeditor;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalizationChecks {
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([-#+ 0,(<]*)\\d*(?:\\.\\d+)?([tT]?[a-zA-Z%])");

    static Set lambda$tokens$0(String k) {
        return new TreeSet();
    }

    public static Map<String, Set<String>> tokens(String value) {
        Map<String, Set<String>> result = new TreeMap<>();
        Matcher m = FORMAT.matcher(value == null ? "" : value);
        int next = 1;
        String last = null;
        while (m.find()) {
            String type = m.group(3);
            if (!type.equals("%") && !type.equals("n")) {
                String index = m.group(1);
                if (index == null) {
                    if (m.group(2).contains("<")) {
                        index = last == null ? "invalid-previous" : last;
                    } else {
                        index = Integer.toString(next);
                        next++;
                    }
                }
                last = index;
                result.computeIfAbsent(index, new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return LocalizationChecks.lambda$tokens$0((String) obj);
                    }
                }).add(type.length() == 1 ? type.toLowerCase(Locale.ROOT) : "t" + type.substring(1));
            }
        }
        return result;
    }
}
