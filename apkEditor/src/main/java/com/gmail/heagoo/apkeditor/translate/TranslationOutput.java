package com.gmail.heagoo.apkeditor.translate;

import java.io.IOException;

/** Remove one complete leading reasoning block, never expose reasoning as a translation. */
public final class TranslationOutput {
    private static final String OPEN = "<think>";
    private static final String CLOSE = "</think>";
    private TranslationOutput() {}

    public static String clean(String source, String output) throws IOException {
        if (output == null) throw new IOException("Translation is empty");
        String text = output.trim();
        // Literal markers in source are resource content, not model protocol.
        if (source.contains(OPEN) || source.contains(CLOSE)) {
            if (text.contains(OPEN) || text.contains(CLOSE))
                throw new IOException("Ambiguous reasoning markers in source");
        } else if (text.startsWith(OPEN)) {
            int end = text.indexOf(CLOSE, OPEN.length());
            if (end < 0 || text.substring(OPEN.length(), end).contains(OPEN))
                throw new IOException("Incomplete or malformed reasoning block");
            text = text.substring(end + CLOSE.length()).trim();
        } else if (text.startsWith(CLOSE)) {
            // Some templates include the opening tag in the assistant prompt.
            // A leading close alone means there is no generated reasoning to discard.
            text = text.substring(CLOSE.length()).trim();
        }
        if (text.isEmpty()) throw new IOException("Translation is empty");
        if (text.contains(OPEN) || text.contains(CLOSE))
            throw new IOException("Unexpected or incomplete reasoning markers");
        if (!TranslationTokens.preserved(source, text))
            throw new IOException("Translation changed a resource placeholder");
        return text;
    }
}
