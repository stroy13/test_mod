package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ui.LayoutObListView;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;
import java.util.Arrays;
import java.util.List;

public class TextEditBigListHelper {
    private static final int MSG_SELECTION = 1000;
    private TextEditBigListAdapter adapter;
    private int backgroundColor;
    private int bracketSpanColor;
    private int complexUnitSp;
    private float fontSize;
    private int inputType;
    private MyHandler myHandler = new MyHandler();
    private int textColor;
    private LayoutObListView textList;

    public TextEditBigListHelper(Context ctx, LayoutObListView textList) {
        this.textList = textList;
        this.adapter = new TextEditBigListAdapter(ctx, textList);
        textList.setAdapter(this.adapter);
        textList.setDivider((Drawable) null);
        textList.setItemsCanFocus(true);
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.textList.setBackgroundColor(backgroundColor);
    }

    public void setTextColor(int textColor) {
        this.adapter.setTextColor(textColor);
    }

    public void setCodeTheme(ColorTheme theme) {
        this.adapter.setCodeTheme(theme);
    }

    public void setBracketSpanColor(int bracketSpanColor) {
        this.bracketSpanColor = bracketSpanColor;
    }

    public void requestFocus() {
    }

    public void selectItem(int itemPos) {
        this.textList.setSelection(itemPos);
    }

    public void setSelection(int itemPosition, int selStart, int selEnd) {
        int firstVisible = this.textList.getFirstVisiblePosition();
        int lastVisible = this.textList.getLastVisiblePosition();
        if (itemPosition < firstVisible || itemPosition > lastVisible) {
            selectItem(itemPosition);
            this.textList.requestFocus();
            this.myHandler.setDelayedSelection(itemPosition, selStart, selEnd);
        } else {
            EditText et = getEditTextByPosition(itemPosition);
            if (et != null) {
                et.requestFocus();
                et.setSelection(selStart, selEnd);
            }
        }
    }

    public int getSelectionItemIndex() {
        int position = this.textList.getSelectedItemPosition();
        if (position == -1) {
            return this.textList.getSelPosition();
        }
        return position;
    }

    public int getSelectionStart() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getSelectionStart();
        }
        return 0;
    }

    public int getSelectionEnd() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getSelectionEnd();
        }
        return 0;
    }

    public EditText getEditTextByPosition(int pos) {
        View view;
        if (pos < 0) {
            return null;
        }
        int firstListItemPosition = this.textList.getFirstVisiblePosition();
        int lastListItemPosition = (this.textList.getChildCount() + firstListItemPosition) - 1;
        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            view = this.textList.getAdapter().getView(pos, null, this.textList);
        } else {
            int childIndex = pos - firstListItemPosition;
            view = this.textList.getChildAt(childIndex);
        }
        if (view == null) {
            return null;
        }
        return (EditText) view.findViewById(R.id.text);
    }

    public CharSequence getText() {
        return this.adapter.getText();
    }

    public void setText(String text) {
        String[] lines = text.split("\\r?\\n");
        this.adapter.setText(Arrays.asList(lines));
        this.adapter.notifyDataSetChanged();
    }

    public void setText(List<String> lines) {
        this.adapter.setText(lines);
        this.adapter.notifyDataSetChanged();
    }

    public List<String> getTextLines() {
        return this.adapter.getTextLines();
    }

    public void replaceSelection(String text) {
        int start;
        int end;
        EditText et;
        int itemIndex = getSelectionItemIndex();
        if (itemIndex >= 0 && (start = getSelectionStart()) != (end = getSelectionEnd()) && (et = getEditTextByPosition(itemIndex)) != null) {
            et.getEditableText().replace(start, end, text);
        }
    }

    public void setTextSize(int complexUnitSp, float fontSize) {
        this.adapter.setTextSize(complexUnitSp, fontSize);
        this.adapter.notifyDataSetChanged();
    }

    public Editable getEditableText() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getEditableText();
        }
        return null;
    }

    public void setMaxWidth(int maxWidth) {
        this.textList.setLayoutParams(new FrameLayout.LayoutParams(maxWidth, -1));
    }

    public void insert(String str) {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            textEt.getText().insert(textEt.getSelectionStart(), str);
        }
    }

    public void requestLayout() {
        this.textList.requestLayout();
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return this.textList.getLayoutParams();
    }

    public void setDocument(Document document) {
        this.adapter.setDocument(document);
    }

    public void showLineNumber(boolean bShow) {
        this.adapter.setShowLineNumbers(bShow);
        this.adapter.notifyDataSetChanged();
    }

    public void setOnTextChangeListener(TextWatcher watcher) {
        this.adapter.setTextChangeListener(watcher);
    }

    public void refresh() {
        this.adapter.notifyDataSetChanged();
    }

    public TextEditBigListAdapter getAdapter() {
        return this.adapter;
    }

    public void setTextSelectionListener(SelectionChangedListener listener) {
        this.adapter.setSelectionChangedListener(listener);
    }

    class MyHandler extends Handler {
        private int itemPosition;
        private int selEnd;
        private int selStart;

        MyHandler() {
        }

        public void setDelayedSelection(int lineIdx, int start, int end) {
            this.itemPosition = lineIdx;
            this.selStart = start;
            this.selEnd = end;
            sendEmptyMessageDelayed(TextEditBigListHelper.MSG_SELECTION, 100L);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TextEditBigListHelper.MSG_SELECTION:
                    EditText et = TextEditBigListHelper.this.getEditTextByPosition(this.itemPosition);
                    if (et != null) {
                        et.requestFocus();
                        et.setSelection(this.selStart, this.selEnd);
                        break;
                    }
                    break;
            }
        }
    }
}
