package com.gmail.heagoo.neweditor;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Layout;
import android.widget.Toast;
import com.gmail.heagoo.neweditor.SyntaxSnapshot;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class AsyncSyntaxController {
    private static final int PREFETCH_LINES = 48;
    private boolean blocked;
    private volatile boolean closed;
    private final Document document;
    private final ObEditText editor;
    private boolean pending;
    private boolean renderPending;
    private SyntaxSnapshot snapshot;
    private boolean warned;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public final Thread newThread(Runnable runnable) {
            return AsyncSyntaxController.lambda$new$0(runnable);
        }
    });
    private final AtomicInteger revision = new AtomicInteger();
    private int appliedStart = -1;
    private int appliedEnd = -1;
    private final Runnable start = new Runnable() {
        @Override
        public final void run() {
            AsyncSyntaxController.this.startAnalysis();
        }
    };
    private final Runnable render = new Runnable() {
        @Override
        public final void run() {
            AsyncSyntaxController.this.render();
        }
    };

    static Thread lambda$new$0(Runnable r) {
        Thread t = new Thread(r, "syntax-parser");
        t.setDaemon(true);
        return t;
    }

    public AsyncSyntaxController(Document doc, ObEditText view) {
        this.document = doc;
        this.editor = view;
    }

    public Document document() {
        return this.document;
    }

    public void request(boolean changed) {
        if (this.closed) {
            return;
        }
        if (changed) {
            this.blocked = false;
            this.revision.incrementAndGet();
            this.snapshot = null;
            this.ui.removeCallbacks(this.start);
            this.pending = true;
            this.ui.postDelayed(this.start, 220L);
        } else if (this.snapshot == null && !this.pending && !this.blocked) {
            this.pending = true;
            this.ui.post(this.start);
        }
        scheduleRender();
    }

    public void startAnalysis() {
        if (this.closed) {
            return;
        }
        this.pending = true;
        final int expected = this.revision.get();
        final String text = this.editor.getText().toString();
        this.worker.execute(new Runnable() {
            @Override
            public final void run() {
                AsyncSyntaxController.this.m377x67b50640(text, expected);
            }
        });
    }

    boolean m375x8461ba02(int expected) {
        return this.closed || this.revision.get() != expected;
    }

    void m376x760b6021(int expected, SyntaxSnapshot ready, boolean failed) {
        if (this.closed || this.revision.get() != expected) {
            return;
        }
        this.pending = false;
        if (ready != null) {
            this.snapshot = ready;
            clearAll();
            this.appliedEnd = -1;
            this.appliedStart = -1;
            scheduleRender();
            return;
        }
        if (failed) {
            this.blocked = true;
            if (this.warned) {
                return;
            }
            this.warned = true;
            Toast.makeText(this.editor.getContext(), "Подсветка недоступна: слишком много строк или ошибка разбора", 1).show();
        }
    }

    void m377x67b50640(String text, final int expected) {
        SyntaxSnapshot parsed = null;
        boolean error = false;
        try {
            parsed = this.document.syntaxSnapshot(text, new BooleanSupplier() {
                @Override
                public final boolean getAsBoolean() {
                    return AsyncSyntaxController.this.m375x8461ba02(expected);
                }
            });
        } catch (RuntimeException e) {
            error = true;
        }
        final SyntaxSnapshot ready = parsed;
        final boolean failed = error;
        this.ui.post(new Runnable() {
            @Override
            public final void run() {
                AsyncSyntaxController.this.m376x760b6021(expected, ready, failed);
            }
        });
    }

    public void scheduleRender() {
        if (this.renderPending || this.closed) {
            return;
        }
        this.renderPending = true;
        this.editor.postOnAnimation(this.render);
    }

    private void clearAll() {
        Editable e = this.editor.getText();
        for (SyntaxHighlightSpan span : (SyntaxHighlightSpan[]) e.getSpans(0, e.length(), SyntaxHighlightSpan.class)) {
            e.removeSpan(span);
        }
    }

    private void clearRange(int first, int last) {
        if (this.snapshot == null || first >= last) {
            return;
        }
        Editable e = this.editor.getText();
        int begin = this.snapshot.lines.get(first).start;
        int end = last < this.snapshot.lines.size() ? this.snapshot.lines.get(last).start : e.length();
        for (SyntaxHighlightSpan span : (SyntaxHighlightSpan[]) e.getSpans(begin, end, SyntaxHighlightSpan.class)) {
            int a = e.getSpanStart(span);
            int b = e.getSpanEnd(span);
            if (a < end && b > begin) {
                e.removeSpan(span);
            }
        }
    }

    private void addRange(int first, int last) {
        Editable e = this.editor.getText();
        for (int i = first; i < last; i++) {
            for (SyntaxSnapshot.Span s : this.snapshot.lines.get(i).spans) {
                if (s.start >= 0 && s.end <= e.length() && s.end > s.start) {
                    e.setSpan(new SyntaxHighlightSpan(s.color), s.start, s.end, 33);
                }
            }
        }
    }

    public void render() {
        this.renderPending = false;
        if (this.closed || this.snapshot == null) {
            return;
        }
        Layout layout = this.editor.getLayout();
        Rect rect = new Rect();
        if (layout == null || !this.editor.getLocalVisibleRect(rect)) {
            this.editor.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    AsyncSyntaxController.this.scheduleRender();
                }
            }, 50L);
            return;
        }
        int top = Math.max(0, rect.top - this.editor.getTotalPaddingTop());
        int bottom = Math.max(top, rect.bottom - this.editor.getTotalPaddingTop());
        int firstOffset = layout.getLineStart(layout.getLineForVertical(top));
        int lastOffset = layout.getLineEnd(layout.getLineForVertical(bottom));
        int first = Math.max(0, this.snapshot.lineAt(firstOffset) - 48);
        int last = Math.min(this.snapshot.lines.size(), this.snapshot.lineAt(lastOffset) + PREFETCH_LINES + 1);
        if (this.appliedStart < 0 || last <= this.appliedStart || first >= this.appliedEnd) {
            if (this.appliedStart >= 0) {
                clearRange(this.appliedStart, this.appliedEnd);
            }
            addRange(first, last);
        } else {
            if (first > this.appliedStart) {
                clearRange(this.appliedStart, first);
            }
            if (last < this.appliedEnd) {
                clearRange(last, this.appliedEnd);
            }
            if (first < this.appliedStart) {
                addRange(first, this.appliedStart);
            }
            if (last > this.appliedEnd) {
                addRange(this.appliedEnd, last);
            }
        }
        this.appliedStart = first;
        this.appliedEnd = last;
    }

    public void close() {
        this.closed = true;
        this.revision.incrementAndGet();
        this.ui.removeCallbacks(this.start);
        this.editor.removeCallbacks(this.render);
        this.worker.shutdownNow();
    }
}
