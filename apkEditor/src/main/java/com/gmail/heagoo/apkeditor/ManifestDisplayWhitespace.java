package com.gmail.heagoo.apkeditor;

/** Display-only replacement of leading ASCII spaces, with unchanged offsets. */
final class ManifestDisplayWhitespace {
    private ManifestDisplayWhitespace() { }

    static char at(CharSequence text, int index) {
        char c = text.charAt(index);
        if (c != ' ') return c;
        // Bound work for deliberately enormous runs of whitespace. Beyond this
        // limit retain ordinary spaces rather than slowing down text layout.
        for (int i = index - 1; i >= 0 && index - i <= 256; i--) {
            char previous = text.charAt(i);
            if (previous == '\n' || previous == '\r') return '\u00a0';
            if (previous != ' ' && previous != '\t') return c;
        }
        return index <= 256 ? '\u00a0' : c;
    }
}
