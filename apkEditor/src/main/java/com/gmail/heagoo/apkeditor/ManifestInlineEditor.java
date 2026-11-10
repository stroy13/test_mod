package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.OverScroller;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import androidx.appcompat.widget.AppCompatEditText;

/** Editable, unmodified XML source with a line-number gutter. */
public final class ManifestInlineEditor extends AppCompatEditText {
    private final Paint gutterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int gutterWidth;
    private ManifestEditorPalette palette;
    private final Runnable highlight = this::highlightNow;
    private final ManifestLineIndex lineIndex = new ManifestLineIndex();
    private static final ExecutorService HIGHLIGHTER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "manifest-highlighter");
        thread.setDaemon(true);
        return thread;
    });
    private Future<?> pendingHighlight;
    private int generation;
    private boolean draggingScrollbar;
    private boolean draggingContent;
    private float downX, downY, lastY;
    private long downTime;
    private VelocityTracker velocity;
    private final OverScroller scroller;
    private final int touchSlop, minFling, maxFling;
    private float dragOffset;
    private final float density;
    private static final class SyntaxColor extends ForegroundColorSpan {
        SyntaxColor(int color) { super(color); }
    }

    public ManifestInlineEditor(Context context, ManifestEditorPalette colors) {
        super(context);
        this.palette = colors;
        density = getResources().getDisplayMetrics().density;
        scroller = new OverScroller(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        minFling = config.getScaledMinimumFlingVelocity();
        maxFling = config.getScaledMaximumFlingVelocity();
        gutterWidth = (int) (52 * density + 0.5f);
        setTypeface(Typeface.MONOSPACE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        setSingleLine(false);
        setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setTypeface(Typeface.MONOSPACE);
        setBackgroundColor(colors.surface);
        setTextColor(colors.text);
        setPadding(gutterWidth + (int) (8 * density), (int) (8 * density), (int) (8 * density), (int) (8 * density));
        setVerticalScrollBarEnabled(false);
        setHorizontallyScrolling(false);
        setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        numberPaint.setTypeface(Typeface.MONOSPACE);
        numberPaint.setTextSize(getTextSize());
        numberPaint.setTextAlign(Paint.Align.RIGHT);
        addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lineIndex.replace(s, start, before, count);
                scheduleHighlight();
            }
            public void afterTextChanged(Editable editable) { }
        });
    }

    private void scheduleHighlight() {
        generation++;
        if (pendingHighlight != null) pendingHighlight.cancel(true);
        removeCallbacks(highlight);
        postDelayed(highlight, 180);
    }

    private void highlightNow() {
        Editable editable = getText();
        Layout layout = getLayout();
        if (editable == null || layout == null || getHeight() == 0) return;
        int top = Math.max(0, getScrollY() - getTotalPaddingTop());
        int firstLine = Math.max(0, layout.getLineForVertical(top) - 8);
        int lastLine = Math.min(layout.getLineCount() - 1,
                layout.getLineForVertical(top + getHeight()) + 8);
        final int first = layout.getLineStart(firstLine);
        final int last = layout.getLineEnd(lastLine);
        final String source = editable.toString();
        final int current = generation;
        pendingHighlight = HIGHLIGHTER.submit(() -> {
            ArrayList<int[]> spans = new ArrayList<>();
            ManifestSyntax.State state = new ManifestSyntax.State();
            int offset = 0;
            while (offset < source.length() && offset <= last) {
                if (Thread.currentThread().isInterrupted()) return;
                int end = source.indexOf('\n', offset);
                if (end < 0) end = source.length();
                String line = source.substring(offset, end);
                for (ManifestSyntax.Span span : ManifestSyntax.tokenize(line, state)) {
                    int start = offset + span.start, stop = offset + span.end;
                    if (stop <= first || start >= last || span.start >= span.end) continue;
                    if (spans.size() >= 2000) break;
                    int color = palette.spanColor(span.kind, line.charAt(span.start));
                    if (span.kind == ManifestSyntax.ATTRIBUTE) {
                        int colon = line.indexOf(':', span.start);
                        if (colon > span.start && colon < span.end) {
                            boolean declaration = line.startsWith("xmlns", span.start);
                            spans.add(new int[]{start, offset + colon, declaration ? palette.attribute : palette.namespace});
                            spans.add(new int[]{offset + colon, offset + colon + 1, palette.text});
                            spans.add(new int[]{offset + colon + 1, stop, declaration ? palette.namespace : palette.attribute});
                            continue;
                        }
                    }
                    spans.add(new int[]{start, stop, color});
                }
                offset = end + 1;
            }
            if (Thread.currentThread().isInterrupted()) return;
            post(() -> {
                if (generation != current || getText() != editable) return;
                for (SyntaxColor span : editable.getSpans(0, editable.length(), SyntaxColor.class)) editable.removeSpan(span);
                for (int[] span : spans) editable.setSpan(new SyntaxColor(span[2]), span[0], span[1], 33);
            });
        });
    }

    @Override protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);
        if (y != oldY && highlight != null) scheduleHighlight();
    }

    @Override protected void onDetachedFromWindow() {
        generation++;
        removeCallbacks(highlight);
        if (pendingHighlight != null) pendingHighlight.cancel(true);
        scroller.abortAnimation();
        endTouch();
        super.onDetachedFromWindow();
    }

    private int scrollRange() {
        Layout layout = getLayout();
        return layout == null ? 0 : Math.max(0, layout.getHeight() + getTotalPaddingTop()
                + getTotalPaddingBottom() - getHeight());
    }
    private float thumbHeight() {
        int range = scrollRange();
        return Math.min(getHeight(), Math.max(36 * density, (float) getHeight() * getHeight() / (getHeight() + range)));
    }
    private float thumbTop() {
        int range = scrollRange();
        return range == 0 ? 0 : (getHeight() - thumbHeight()) * getScrollY() / range;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            scroller.abortAnimation();
            draggingContent = false;
            downX = event.getX(); downY = lastY = event.getY(); downTime = event.getEventTime();
            if (velocity != null) velocity.recycle();
            velocity = VelocityTracker.obtain();
            velocity.addMovement(event);
            // Keep the editor's gesture even when a containing view can scroll.
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            if (scrollRange() > 0 && downX >= getWidth() - 32 * density
                    && downY >= thumbTop() - 16 * density
                    && downY <= thumbTop() + thumbHeight() + 16 * density) {
                draggingScrollbar = true;
                dragOffset = downY - thumbTop();
                return true;
            }
            return super.onTouchEvent(event);
        }
        if (velocity != null) velocity.addMovement(event);
        if (draggingScrollbar) {
            if (action == MotionEvent.ACTION_MOVE) {
                float track = getHeight() - thumbHeight();
                float fraction = track <= 0 ? 0 : Math.max(0, Math.min(1, (event.getY() - dragOffset) / track));
                scrollTo(getScrollX(), Math.round(fraction * scrollRange()));
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                draggingScrollbar = false;
                endTouch();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dy = event.getY() - downY;
            if (!draggingContent && ManifestScrollGesture.startsScroll(event.getX() - downX, dy,
                    touchSlop, event.getEventTime() - downTime, ViewConfiguration.getLongPressTimeout(), scrollRange())) {
                // Cancel TextView's tap/selection gesture, then handle scrolling ourselves.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.onTouchEvent(cancel);
                cancel.recycle();
                draggingContent = true;
                lastY = downY;
            }
            if (draggingContent) {
                int y = ManifestScrollGesture.clamp(Math.round(getScrollY() + lastY - event.getY()), scrollRange());
                scrollTo(getScrollX(), y);
                lastY = event.getY();
                return true;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (draggingContent) {
                if (action == MotionEvent.ACTION_UP && velocity != null) {
                    velocity.computeCurrentVelocity(1000, maxFling);
                    int speed = Math.round(-velocity.getYVelocity());
                    if (Math.abs(speed) >= minFling) {
                        scroller.fling(getScrollX(), getScrollY(), 0, speed,
                                getScrollX(), getScrollX(), 0, scrollRange());
                        postInvalidateOnAnimation();
                    }
                }
                draggingContent = false;
                endTouch();
                return true;
            }
            boolean result = super.onTouchEvent(event);
            endTouch();
            return result;
        }
        return super.onTouchEvent(event);
    }

    private void endTouch() {
        if (velocity != null) { velocity.recycle(); velocity = null; }
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }

    @Override public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(getScrollX(), Math.max(0, Math.min(scrollRange(), scroller.getCurrY())));
            postInvalidateOnAnimation();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        if (layout != null) {
            float x = getScrollX();
            gutterPaint.setColor(palette.gutter);
            canvas.drawRect(x, getScrollY(), x + gutterWidth, getScrollY() + getHeight(), gutterPaint);
            gutterPaint.setColor(palette.gutterBorder);
            canvas.drawRect(x + gutterWidth - 1, getScrollY(), x + gutterWidth, getScrollY() + getHeight(), gutterPaint);
            numberPaint.setColor(palette.gutterText);
            int first = layout.getLineForVertical(Math.max(0, getScrollY() - getTotalPaddingTop()));
            int last = layout.getLineForVertical(getScrollY() + getHeight());
            CharSequence text = getText();
            int physical = lineIndex.lineAt(layout.getLineStart(first));
            for (int line = first; line <= last && line < layout.getLineCount(); line++) {
                int lineStart = layout.getLineStart(line);
                if (lineStart == 0 || text.charAt(lineStart - 1) == '\n') {
                    canvas.drawText(Integer.toString(physical), x + gutterWidth - 6,
                            getTotalPaddingTop() + layout.getLineBaseline(line), numberPaint);
                }
                int lineEnd = layout.getLineEnd(line);
                if (lineStart == 0 || text.charAt(lineStart - 1) == '\n') {
                    int indent = 0;
                    while (lineStart + indent < lineEnd && text.charAt(lineStart + indent) == ' ') indent++;
                    gutterPaint.setColor(palette.indentGuide);
                    float space = getPaint().measureText(" ");
                    for (int column = 4; column <= indent && column <= 80; column += 4) {
                        float guideX = getTotalPaddingLeft() + column * space;
                        canvas.drawRect(guideX, getTotalPaddingTop() + layout.getLineTop(line),
                                guideX + density, getTotalPaddingTop() + layout.getLineBottom(line), gutterPaint);
                    }
                }
                if (lineEnd > lineStart && text.charAt(lineEnd - 1) == '\n') physical++;
            }
        }
        super.onDraw(canvas);
        if (scrollRange() > 0) {
            gutterPaint.setColor(palette.dark ? 0xAA9DA4AE : 0xAA707780);
            float right = getScrollX() + getWidth() - 3 * density;
            float top = getScrollY() + thumbTop();
            canvas.drawRoundRect(right - 5 * density, top, right, top + thumbHeight(),
                    3 * density, 3 * density, gutterPaint);
        }
    }
}
