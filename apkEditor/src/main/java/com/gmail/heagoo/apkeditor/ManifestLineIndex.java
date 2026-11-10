package com.gmail.heagoo.apkeditor;

import java.util.ArrayList;
import java.util.Collections;

/** Physical line starts, updated from TextWatcher's replacement range. */
final class ManifestLineIndex {
    private final ArrayList<Integer> starts = new ArrayList<>();

    ManifestLineIndex() { starts.add(0); }

    void replace(CharSequence text, int start, int before, int count) {
        int oldEnd = start + before;
        int delta = count - before;
        int first = upperBound(start);
        int end = upperBound(oldEnd);
        starts.subList(first, end).clear();
        for (int i = first; i < starts.size(); i++) starts.set(i, starts.get(i) + delta);
        int insert = first;
        for (int i = start; i < start + count; i++) {
            if (text.charAt(i) == '\n') starts.add(insert++, i + 1);
        }
    }

    int lineAt(int offset) { return upperBound(offset); }

    private int upperBound(int offset) {
        int i = Collections.binarySearch(starts, offset);
        return i >= 0 ? i + 1 : -i - 1;
    }
}
