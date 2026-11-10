package com.gmail.heagoo.apkeditor;

import android.graphics.Rect;
import android.text.GetChars;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.TransformationMethod;
import android.view.View;

/** Keeps leading indentation attached to the following word for visual wrapping.
 * This is a same-length live view: the Editable, selection offsets and saved XML
 * retain their original ordinary spaces. */
final class ManifestIndentTransformation implements TransformationMethod {
    @Override public CharSequence getTransformation(CharSequence source, View view) {
        return source instanceof Spanned ? new Styled((Spanned) source, 0, -1)
                : new Display(source, 0, -1);
    }
    @Override public void onFocusChanged(View view, CharSequence source, boolean focused,
                                         int direction, Rect previouslyFocusedRect) { }

    private static class Display implements CharSequence, GetChars {
        final CharSequence source;
        final int start, end;
        Display(CharSequence source, int start, int end) { this.source = source; this.start = start; this.end = end; }
        @Override public int length() { return (end < 0 ? source.length() : end) - start; }
        @Override public char charAt(int index) {
            if (index < 0 || index >= length()) throw new IndexOutOfBoundsException();
            return ManifestDisplayWhitespace.at(source, start + index);
        }
        @Override public void getChars(int from, int to, char[] dest, int destoff) {
            if (from < 0 || to < from || to > length()) throw new IndexOutOfBoundsException();
            for (int i = from; i < to; i++) dest[destoff++] = charAt(i);
        }
        @Override public CharSequence subSequence(int from, int to) {
            if (from < 0 || to < from || to > length()) throw new IndexOutOfBoundsException();
            return new Display(source, start + from, start + to);
        }
        @Override public String toString() {
            char[] chars = new char[length()];
            getChars(0, chars.length, chars, 0);
            return new String(chars);
        }
    }
    private static final class Styled extends Display implements Spanned {
        private final Spanned styled;
        Styled(Spanned source, int start, int end) { super(source, start, end); styled = source; }
        @Override public <T> T[] getSpans(int from, int to, Class<T> type) {
            return styled.getSpans(start + from, start + to, type);
        }
        @Override public int getSpanStart(Object span) {
            int value = styled.getSpanStart(span);
            return value < 0 ? -1 : Math.max(0, value - start);
        }
        @Override public int getSpanEnd(Object span) {
            int value = styled.getSpanEnd(span);
            return value < 0 ? -1 : Math.min(length(), value - start);
        }
        @Override public int getSpanFlags(Object span) { return styled.getSpanFlags(span); }
        @Override public int nextSpanTransition(int from, int limit, Class type) {
            return styled.nextSpanTransition(start + from, start + limit, type) - start;
        }
        @Override public CharSequence subSequence(int from, int to) {
            if (from < 0 || to < from || to > length()) throw new IndexOutOfBoundsException();
            // Copy only on explicit subsequence calls; retain original span objects
            // and local offsets just as Spanned.subSequence does.
            Styled slice = new Styled(styled, start + from, start + to);
            return new SpannableString(slice);
        }
    }
}
