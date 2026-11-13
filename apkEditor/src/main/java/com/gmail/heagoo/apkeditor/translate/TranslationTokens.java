package com.gmail.heagoo.apkeditor.translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative check for Android resource syntax that a translator must preserve.
 * This is a safety check, not a parser or a guarantee of a correct translation. */
public final class TranslationTokens {
    private static final Pattern TOKENS = Pattern.compile(
            "%(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?(?:[tT][a-zA-Z]|[a-zA-Z%])"
            + "|\\\\(?:u[0-9a-fA-F]{4}|.)"
            + "|</?[a-zA-Z][^>]*>"
            + "|&(?:#[0-9]+|#x[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);"
            + "|[?@](?:[a-zA-Z0-9_.]+:)?[a-zA-Z][a-zA-Z0-9_]*/[a-zA-Z0-9_.]+"
    );
    private TranslationTokens() {}

    /** Checks multiplicity as well as spelling; callers must still validate XML and semantics. */
    public static boolean preserved(String source, String translated) {
        if (source == null || translated == null) return false;
        return tokens(source).equals(tokens(translated))
                && implicitFormats(source).equals(implicitFormats(translated));
    }

    private static List<String> implicitFormats(String text) {
        List<String> formats = new ArrayList<>();
        Matcher matcher = TOKENS.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith("%") && !token.matches("%\\d+\\$.*")
                    && !token.equals("%%") && !token.equals("%n")) formats.add(token);
        }
        return formats;
    }

    private static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKENS.matcher(text);
        while (matcher.find()) tokens.add(matcher.group());
        Collections.sort(tokens);
        return tokens;
    }
}
