package com.gmail.heagoo.apkeditor;

import java.util.ArrayList;
import java.util.List;
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

    /** Renders positional placeholders as users write them in XML, for example {@code %1$s, %2$d}. */
    public static String formatTokens(Map<String, Set<String>> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "нет";
        }
        List<String> indexes = new ArrayList<>(tokens.keySet());
        indexes.sort((left, right) -> {
            boolean leftNumber = left.matches("\\d+");
            boolean rightNumber = right.matches("\\d+");
            if (leftNumber && rightNumber) {
                String leftKey = left.replaceFirst("^0+", "");
                String rightKey = right.replaceFirst("^0+", "");
                if (leftKey.isEmpty()) {
                    leftKey = "0";
                }
                if (rightKey.isEmpty()) {
                    rightKey = "0";
                }
                int byLength = Integer.compare(leftKey.length(), rightKey.length());
                return byLength != 0 ? byLength : leftKey.compareTo(rightKey);
            }
            if (leftNumber != rightNumber) {
                return leftNumber ? -1 : 1;
            }
            return left.compareTo(right);
        });
        StringBuilder out = new StringBuilder();
        for (String index : indexes) {
            for (String type : new TreeSet<>(tokens.get(index))) {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append('%').append(index).append('$').append(type);
            }
        }
        return out.length() == 0 ? "нет" : out.toString();
    }
}
