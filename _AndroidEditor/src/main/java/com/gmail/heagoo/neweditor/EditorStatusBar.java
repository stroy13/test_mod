package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public final class EditorStatusBar extends LinearLayout {
    private ObEditText attachedEditor;
    private final Paint dividerPaint;
    private final TextView fileView;
    private final LogicalLineIndex lineIndex;
    private final TextView positionView;
    private final TextView syntaxView;

    public EditorStatusBar(Context context) {
        this(context, null);
    }

    public EditorStatusBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditorStatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.dividerPaint = new Paint();
        this.lineIndex = new LogicalLineIndex();
        setOrientation(0);
        setGravity(16);
        int horizontal = dp(10);
        setPadding(horizontal, 0, horizontal, 0);
        ColorTheme theme = new ColorTheme(context);
        setBackgroundColor(theme.getGutterColor());
        this.dividerPaint.setColor(theme.getDividerColor());
        this.fileView = makeText(context, theme.getForeground(), false);
        this.fileView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        addView(this.fileView, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.positionView = makeText(context, theme.getLineNumberColor(), false);
        this.positionView.setGravity(8388613);
        this.positionView.setMinWidth(dp(58));
        addView(this.positionView, new LinearLayout.LayoutParams(-2, -2));
        this.syntaxView = makeText(context, theme.getStatusSyntaxColor(), true);
        this.syntaxView.setPadding(dp(12), 0, 0, 0);
        addView(this.syntaxView, new LinearLayout.LayoutParams(-2, -2));
        setPosition(1, 1);
        setSyntaxFromFile(null, null);
    }

    private TextView makeText(Context context, int i, boolean z) {
        TextView textView = new TextView(context);
        textView.setSingleLine(true);
        textView.setTextSize(11.0f);
        textView.setTextColor(i);
        textView.setTypeface(Typeface.MONOSPACE, z ? 1 : 0);
        return textView;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void setFileName(String name) {
        this.fileView.setText(name == null ? "" : name);
    }

    public void setPosition(int line, int column) {
        this.positionView.setText(String.format(Locale.ROOT, "%d:%d", Integer.valueOf(Math.max(1, line)), Integer.valueOf(Math.max(1, column))));
    }

    public void attachEditor(final ObEditText editor) {
        if (this.attachedEditor == editor) {
            return;
        }
        this.attachedEditor = editor;
        this.lineIndex.reset(editor.getText());
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EditorStatusBar.this.lineIndex.changed(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                EditorStatusBar.this.updatePosition(editor.getSelectionStart());
            }
        });
        updatePosition(editor.getSelectionStart());
    }

    public void updatePosition(int offset) {
        if (this.attachedEditor == null) {
            setPosition(1, 1);
        } else {
            setPosition(this.lineIndex.line(offset), this.lineIndex.column(offset));
        }
    }

    public void setSyntaxFromFile(String filePath, String syntaxFileName) {
        String label;
        String syntax = syntaxFileName;
        if (syntax == null) {
            String name = filePath == null ? "" : filePath;
            int slash = Math.max(name.lastIndexOf(47), name.lastIndexOf(92));
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            int dot = name.lastIndexOf(46);
            syntax = dot >= 0 ? name.substring(dot + 1) + ".xml" : "txt.xml";
        }
        String lower = syntax.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xml")) {
            label = lower.substring(0, lower.length() - 4);
        } else {
            label = lower;
        }
        if ("txt".equals(label) || label.isEmpty()) {
            label = "text";
        } else if ("htm".equals(label)) {
            label = "html";
        }
        if ("xml".equals(label) && filePath != null && filePath.endsWith("AndroidManifest.xml")) {
            label = "axml";
        }
        this.syntaxView.setText(label.toUpperCase(Locale.ROOT));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawRect(0.0f, getHeight() - 1, getWidth(), getHeight(), this.dividerPaint);
    }
}
