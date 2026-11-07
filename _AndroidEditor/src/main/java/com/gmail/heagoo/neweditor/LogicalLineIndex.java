package com.gmail.heagoo.neweditor;

import java.util.ArrayList;

public final class LogicalLineIndex {
    private int length;
    private ArrayList<Integer> starts = new ArrayList<>();

    public LogicalLineIndex() {
        reset("");
    }

    public LogicalLineIndex(CharSequence text) {
        reset(text);
    }

    public final void reset(CharSequence text) {
        this.starts.clear();
        this.starts.add(0);
        int size = text != null ? text.length() : 0;
        for (int i = 0; i < size; i++) {
            if (text.charAt(i) == '\n') {
                this.starts.add(Integer.valueOf(i + 1));
            }
        }
        this.length = size;
    }

    public final void changed(CharSequence text, int start, int before, int count) {
        int end = start + before;
        int delta = count - before;
        if (text == null || start < 0 || before < 0 || count < 0 || end > this.length || text.length() != this.length + delta) {
            reset(text);
            return;
        }
        int from = upperBound(start);
        int to = upperBound(end);
        int newlineCount = 0;
        for (int i = start; i < start + count; i++) {
            if (text.charAt(i) == '\n') {
                newlineCount++;
            }
        }
        ArrayList<Integer> inserted = null;
        if (newlineCount > 0) {
            inserted = new ArrayList<>(newlineCount);
            for (int i2 = start; i2 < start + count; i2++) {
                if (text.charAt(i2) == '\n') {
                    inserted.add(Integer.valueOf(i2 + 1));
                }
            }
        }
        int i3 = to - from;
        for (int i4 = to; i4 < this.starts.size(); i4++) {
            this.starts.set(i4 - i3, Integer.valueOf(this.starts.get(i4).intValue() + delta));
        }
        for (int i5 = 0; i5 < i3; i5++) {
            this.starts.remove(this.starts.size() - 1);
        }
        if (inserted != null) {
            this.starts.addAll(from, inserted);
        }
        this.length = text.length();
    }

    public int line(int offset) {
        int index = lineIndex(offset);
        return index + 1;
    }

    public int column(int offset) {
        int clamped = clamp(offset);
        return (clamped - this.starts.get(lineIndex(clamped)).intValue()) + 1;
    }

    public int lineCount() {
        return this.starts.size();
    }

    private int lineIndex(int offset) {
        return Math.max(0, upperBound(clamp(offset)) - 1);
    }

    private int upperBound(int offset) {
        int low = 0;
        int high = this.starts.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (this.starts.get(mid).intValue() <= offset) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private int clamp(int offset) {
        return Math.max(0, Math.min(offset, this.length));
    }
}
