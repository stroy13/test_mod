package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.lang.ref.WeakReference;

public class ObEditText extends EditText implements InputMethodWatcher {
    private boolean arrowKeyPressed;
    private boolean bInputMethodVisible;
    private BackgroundColorSpan closeBracketSpan;
    private final Paint currentLinePaint;
    private boolean editable;
    private final Paint indentGuidePaint;
    private int lastColumn;
    private int lastHighlightLine;
    private int lastLine;
    private int lastPosition;
    private Rect mBounds;
    private int mNumberCols;
    private boolean mNumberColsChanged;
    private int mNumberLines;
    private boolean mWrapped;
    private BackgroundColorSpan openBracketSpan;
    private boolean positionHack;
    private int secLastPosition;
    private boolean showCurrentLine;
    private boolean showIndentGuides;
    private WeakReference<TextSelectionListener> textSelectionListener;

    public ObEditText(Context context) {
        super(context);
        this.closeBracketSpan = new BackgroundColorSpan(-256);
        this.currentLinePaint = new Paint();
        this.indentGuidePaint = new Paint();
        this.lastHighlightLine = -1;
        this.editable = true;
        this.lastColumn = -1;
        this.lastLine = -1;
        this.mBounds = new Rect();
        this.openBracketSpan = new BackgroundColorSpan(-256);
        this.positionHack = false;
        this.textSelectionListener = null;
    }

    public ObEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.closeBracketSpan = new BackgroundColorSpan(-256);
        this.currentLinePaint = new Paint();
        this.indentGuidePaint = new Paint();
        this.lastHighlightLine = -1;
        this.editable = true;
        this.lastColumn = -1;
        this.lastLine = -1;
        this.mBounds = new Rect();
        this.openBracketSpan = new BackgroundColorSpan(-256);
        this.positionHack = false;
        this.textSelectionListener = null;
    }

    public ObEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.closeBracketSpan = new BackgroundColorSpan(-256);
        this.currentLinePaint = new Paint();
        this.indentGuidePaint = new Paint();
        this.lastHighlightLine = -1;
        this.editable = true;
        this.lastColumn = -1;
        this.lastLine = -1;
        this.mBounds = new Rect();
        this.openBracketSpan = new BackgroundColorSpan(-256);
        this.positionHack = false;
        this.textSelectionListener = null;
    }

    public void setPositionHack(boolean positionHack) {
        this.positionHack = positionHack;
    }

    public void setBracketSpanColor(int color) {
        this.openBracketSpan = new BackgroundColorSpan(color);
        this.closeBracketSpan = new BackgroundColorSpan(color);
    }

    public void setCurrentLineHighlightColor(int color) {
        this.currentLinePaint.setColor(color);
        this.currentLinePaint.setStyle(Paint.Style.FILL);
        this.showCurrentLine = true;
        invalidate();
    }

    public void setIndentGuideColor(int color) {
        this.indentGuidePaint.setColor(color);
        this.indentGuidePaint.setStrokeWidth(1.0f);
        this.indentGuidePaint.setAntiAlias(false);
        this.showIndentGuides = true;
        invalidate();
    }

    public void setTextSelectionListener(TextSelectionListener textSelectionListener) {
        this.textSelectionListener = new WeakReference<>(textSelectionListener);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        int line;
        TextSelectionListener listener;
        super.onSelectionChanged(selStart, selEnd);
        if (this.textSelectionListener != null && (listener = this.textSelectionListener.get()) != null) {
            listener.selectionChanged(selStart, selEnd);
        }
        if (this.showCurrentLine && getLayout() != null && selStart >= 0 && (line = getLayout().getLineForOffset(Math.min(selStart, getText().length()))) != this.lastHighlightLine) {
            this.lastHighlightLine = line;
            invalidate();
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (this.showCurrentLine) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Canvas canvas2;
        Layout layout = getLayout();
        if (layout == null) {
            canvas2 = canvas;
        } else if (this.showCurrentLine || this.showIndentGuides) {
            if (!this.showCurrentLine || !isFocused()) {
                canvas2 = canvas;
            } else {
                int selection = Math.max(0, Math.min(getSelectionStart(), getText().length()));
                int line = layout.getLineForOffset(selection);
                int top = getTotalPaddingTop() + layout.getLineTop(line);
                int bottom = getTotalPaddingTop() + layout.getLineBottom(line);
                canvas2 = canvas;
                canvas2.drawRect(0.0f, top, getWidth(), bottom, this.currentLinePaint);
            }
            if (this.showIndentGuides) {
                drawIndentGuides(canvas2, layout);
            }
        } else {
            canvas2 = canvas;
        }
        super.onDraw(canvas2);
    }

    private void drawIndentGuides(Canvas canvas, Layout layout) {
        CharSequence text;
        Layout layout2 = layout;
        CharSequence text2 = getText();
        if (text2 != null && text2.length() != 0) {
            if (getWidth() == 0) {
                return;
            }
            Rect visible = this.mBounds;
            if (getLocalVisibleRect(visible)) {
                int top = Math.max(0, visible.top - getTotalPaddingTop());
                int bottom = Math.max(top, visible.bottom - getTotalPaddingTop());
                int first = Math.max(0, layout2.getLineForVertical(top));
                int last = Math.min(layout2.getLineCount() - 1, layout2.getLineForVertical(bottom));
                float spaceWidth = getPaint().measureText(" ");
                if (spaceWidth <= 0.0f) {
                    return;
                }
                int line = first;
                while (line <= last) {
                    int start = layout2.getLineStart(line);
                    if (start > 0 && text2.charAt(start - 1) != '\n') {
                        text = text2;
                    } else {
                        int end = layout2.getLineEnd(line);
                        int columns = 0;
                        for (int offset = start; offset < end && offset < text2.length(); offset++) {
                            char c = text2.charAt(offset);
                            if (c == ' ') {
                                columns++;
                            } else if (c != '\t') {
                                break;
                            } else {
                                columns += 4;
                            }
                        }
                        int lineTop = getTotalPaddingTop() + layout2.getLineTop(line);
                        int lineBottom = getTotalPaddingTop() + layout2.getLineBottom(line);
                        int column = 4;
                        while (true) {
                            if (column >= columns) {
                                text = text2;
                                break;
                            }
                            text = text2;
                            float x = getTotalPaddingLeft() + (column * spaceWidth);
                            if (x > getWidth()) {
                                break;
                            }
                            canvas.drawLine(x, lineTop, x, lineBottom, this.indentGuidePaint);
                            column += 4;
                            text2 = text;
                        }
                    }
                    line++;
                    layout2 = layout;
                    text2 = text;
                }
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (getSelectionStart() == getSelectionEnd()) {
            checkMatchingBracket(getSelectionStart());
        }
    }

    private void checkMatchingBracket(int pos) {
    }

    private void showBracket(int i, int j) {
        getText().setSpan(this.openBracketSpan, i, i + 1, 33);
        getText().setSpan(this.closeBracketSpan, j, j + 1, 33);
    }

    public void setSelectionNoHack(int selectionStart, int selectionEnd) {
        boolean oldPositionHack = this.positionHack;
        this.positionHack = false;
        try {
            setSelection(selectionStart, selectionEnd);
        } catch (Exception e) {
        }
        this.positionHack = oldPositionHack;
    }

    public int getLineNumber(int offset) {
        if (getLayout() != null) {
            return ((DynamicLayout) getLayout()).getLineForOffset(offset);
        }
        return 0;
    }

    private int getLineEnd(int line) {
        return ((DynamicLayout) getLayout()).getLineEnd(line) - 1;
    }

    private int getLineStart(int line) {
        return ((DynamicLayout) getLayout()).getLineStart(line);
    }

    @Override
    public void setWidth(int pixels) {
        if (pixels != 0) {
            super.setWidth(pixels);
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return isEditable();
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setArrowKeyPressed(boolean arrowKeyPressed) {
        this.arrowKeyPressed = arrowKeyPressed;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        boolean z = false;
        if (!PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("rawKeys", false)) {
            return super.onCreateInputConnection(outAttrs);
        }
        BaseInputConnection fic = new BaseInputConnection(this, z) {
            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (beforeLength == 1 && afterLength == 0) {
                    return super.sendKeyEvent(new KeyEvent(0, 67)) && super.sendKeyEvent(new KeyEvent(1, 67));
                }
                return super.deleteSurroundingText(beforeLength, afterLength);
            }
        };
        outAttrs.actionLabel = null;
        outAttrs.inputType = 0;
        outAttrs.imeOptions = 1342177280;
        return fic;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mWrapped) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        float width = getPaint().measureText("X");
        int height = getLineHeight();
        updateFastLineCount();
        setMeasuredDimension(Math.max(((int) ((String.valueOf(this.mNumberLines).length() + this.mNumberCols) * width)) + 10, View.MeasureSpec.getSize(widthMeasureSpec)), Math.max((this.mNumberLines * height) + 10, View.MeasureSpec.getSize(heightMeasureSpec)));
        if (this.mNumberColsChanged) {
            requestReflow();
        }
    }

    public void updateFastLineCount() {
        boolean z = true;
        String[] lines = getText().toString().split("\\n");
        int oldNumberCols = this.mNumberCols;
        this.mNumberCols = 0;
        this.mNumberLines = lines.length;
        for (String string : lines) {
            this.mNumberCols = Math.max(this.mNumberCols, string.length());
        }
        if (this.mNumberLines == 0) {
            this.mNumberLines = 1;
        }
        if (oldNumberCols == this.mNumberCols) {
            z = false;
        }
        this.mNumberColsChanged = z;
    }

    public final void fastMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setWrapped(boolean wrapped) {
        this.mWrapped = wrapped;
    }

    public void requestReflow() {
        setPaintFlags(getPaintFlags() + 1);
        setPaintFlags(getPaintFlags() - 1);
    }

    @Override
    public void setInputMethodVisible(boolean visible) {
        this.bInputMethodVisible = visible;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode != 4 || !this.bInputMethodVisible) {
            return false;
        }
        InputMethodManager mgr = (InputMethodManager) getContext().getSystemService("input_method");
        mgr.hideSoftInputFromWindow(getWindowToken(), 0);
        return true;
    }
}
