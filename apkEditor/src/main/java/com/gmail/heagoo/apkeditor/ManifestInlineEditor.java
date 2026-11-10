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
import androidx.appcompat.widget.AppCompatEditText;

/** Editable, unmodified XML source with a line-number gutter. */
public final class ManifestInlineEditor extends AppCompatEditText {
    private final Paint gutterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int gutterWidth;
    private ManifestEditorPalette palette;
    private final Runnable highlight = this::highlightNow;

    public ManifestInlineEditor(Context context, ManifestEditorPalette colors) {
        super(context);
        this.palette = colors;
        float density = getResources().getDisplayMetrics().density;
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
        setVerticalScrollBarEnabled(true);
        setHorizontallyScrolling(false);
        numberPaint.setTypeface(Typeface.MONOSPACE);
        numberPaint.setTextSize(getTextSize());
        numberPaint.setTextAlign(Paint.Align.RIGHT);
        addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                removeCallbacks(highlight);
                postDelayed(highlight, 350);
            }
            public void afterTextChanged(Editable editable) { }
        });
    }

    private void highlightNow() {
        Editable editable = getText();
        if (editable == null) return;
        for (ForegroundColorSpan span : editable.getSpans(0, editable.length(), ForegroundColorSpan.class)) {
            editable.removeSpan(span);
        }
        // Avoid allocating thousands of spans while typing very large manifests.
        if (editable.length() > 200000) return;
        String source = editable.toString();
        ManifestSyntax.State state = new ManifestSyntax.State();
        int offset = 0;
        while (offset < source.length()) {
            int end = source.indexOf('\n', offset);
            if (end < 0) end = source.length();
            String line = source.substring(offset, end);
            for (ManifestSyntax.Span span : ManifestSyntax.tokenize(line, state)) {
                if (span.start < span.end && span.end <= line.length()) {
                    int color = palette.spanColor(span.kind, line.charAt(span.start));
                    editable.setSpan(new ForegroundColorSpan(color), offset + span.start, offset + span.end, 33);
                }
            }
            offset = end + 1;
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
            int physical = 1;
            int start = layout.getLineStart(first);
            for (int i = 0; i < start; i++) if (text.charAt(i) == '\n') physical++;
            for (int line = first; line <= last && line < layout.getLineCount(); line++) {
                int lineStart = layout.getLineStart(line);
                if (lineStart == 0 || text.charAt(lineStart - 1) == '\n') {
                    canvas.drawText(Integer.toString(physical), x + gutterWidth - 6,
                            getTotalPaddingTop() + layout.getLineBaseline(line), numberPaint);
                }
                int lineEnd = layout.getLineEnd(line);
                if (lineEnd > lineStart && text.charAt(lineEnd - 1) == '\n') physical++;
            }
        }
        super.onDraw(canvas);
    }
}
