package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ui.EditTextRememberCursor;
import com.gmail.heagoo.apkeditor.ui.LayoutObListView;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TextEditBigListAdapter extends BaseAdapter {
    private int complexUnitSp;
    private Context ctx;
    private Document document;
    private float fontSize;
    private WeakReference<LayoutObListView> listRef;
    private WeakReference<SelectionChangedListener> selectionListenerRef;
    private TextWatcher textChangeListener;
    private int textColor = -1;
    private int backgroundColor = -14671580;
    private int gutterColor = -15000546;
    private int lineNumberColor = -8025197;
    private int dividerColor = -13289669;
    private int currentLineColor = -14079185;
    private int selectedLine = -1;
    private boolean bShowLineNumbers = true;
    private MyHandler handler = new MyHandler();
    private List<String> lines = new ArrayList();

    public TextEditBigListAdapter(Context ctx, LayoutObListView list) {
        this.ctx = ctx;
        this.listRef = new WeakReference<>(list);
    }

    public void setSelectionChangedListener(SelectionChangedListener selectionChangedListener) {
        this.selectionListenerRef = new WeakReference<>(selectionChangedListener);
    }

    @Override
    public int getCount() {
        return this.lines.size();
    }

    @Override
    public Object getItem(int position) {
        return this.lines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private void reclaimFocus(View v, long timestamp) {
        if (timestamp != -1 && System.currentTimeMillis() - timestamp < 250) {
            v.requestFocus();
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.ctx).inflate(R.layout.item_lined_text, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.root = convertView;
            viewHolder.lineTv = (TextView) convertView.findViewById(R.id.line_num);
            viewHolder.divider = convertView.findViewById(R.id.line_divider);
            viewHolder.et = convertView.findViewById(R.id.text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Object tag = viewHolder.et.getTag();
        if (tag != null) {
            viewHolder.et.removeTextChangedListener((CustomTextWatcher) tag);
        }
        CustomTextWatcher watcher = new CustomTextWatcher(position);
        viewHolder.et.addTextChangedListener(watcher);
        viewHolder.et.setTag(watcher);
        viewHolder.et.setTextSelectionListener(this.selectionListenerRef);
        String strNum = getNumString(position + 1);
        if (this.bShowLineNumbers) {
            viewHolder.lineTv.setVisibility(0);
            viewHolder.divider.setVisibility(0);
            viewHolder.lineTv.setText(strNum);
        } else {
            viewHolder.lineTv.setVisibility(8);
            viewHolder.divider.setVisibility(8);
        }
        viewHolder.et.setTextColor(this.textColor);
        viewHolder.et.setText(this.lines.get(position));
        if (this.fontSize > 0.0f) {
            viewHolder.lineTv.setTextSize(this.complexUnitSp, this.fontSize);
            viewHolder.et.setTextSize(this.complexUnitSp, this.fontSize);
        }
        if (this.document != null) {
            this.document.syntaxHighlight(viewHolder.et);
        }
        viewHolder.et.setTag(R.id.text, Integer.valueOf(position));
        viewHolder.et.setParent(this.ctx, this.listRef);
        viewHolder.et.selectionClickable();
        applyRowColors(viewHolder, position);
        viewHolder.et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public final void onFocusChange(View view, boolean z) {
                TextEditBigListAdapter.this.m322x7b561881(position, view, z);
            }
        });
        LayoutObListView lv = this.listRef.get();
        if (lv != null && lv.getSelPosition() == position) {
            viewHolder.et.setSelection(lv.getSelStart(), lv.getSelEnd());
            viewHolder.et.requestFocus();
            this.handler.setTargetView(viewHolder.et);
            this.handler.removeMessages(0);
            this.handler.sendEmptyMessageDelayed(0, 100L);
        }
        return convertView;
    }

    void m322x7b561881(int position, View v, boolean hasFocus) {
        if (hasFocus) {
            this.selectedLine = position;
            updateVisibleRows();
        }
    }

    private void applyRowColors(ViewHolder holder, int position) {
        boolean current = position == this.selectedLine;
        holder.root.setBackgroundColor(current ? this.currentLineColor : this.backgroundColor);
        holder.lineTv.setBackgroundColor(this.gutterColor);
        holder.lineTv.setTextColor(current ? this.textColor : this.lineNumberColor);
        holder.divider.setBackgroundColor(this.dividerColor);
    }

    private void updateVisibleRows() {
        LayoutObListView list = this.listRef.get();
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof ViewHolder) {
                ViewHolder holder = (ViewHolder) tag;
                Object position = holder.et.getTag(R.id.text);
                if (position instanceof Integer) {
                    applyRowColors(holder, ((Integer) position).intValue());
                }
            }
        }
    }

    private int getNumberDigits(int lines) {
        int nd = 1;
        while (lines >= 10) {
            lines /= 10;
            nd++;
        }
        return nd;
    }

    private String getPaddingString(int digits) {
        switch (digits) {
            case 1:
                return "0";
            case 2:
                return "00";
            case 3:
                return "000";
            case 4:
                return "0000";
            case 5:
                return "00000";
            case 6:
                return "000000";
            default:
                return "0000000";
        }
    }

    private String getNumString(int num) {
        int digits = getNumberDigits(this.lines.size());
        int cur = getNumberDigits(num);
        if (cur < digits) {
            return getPaddingString(digits - cur) + num;
        }
        return "" + num;
    }

    public void setTextSize(int complexUnitSp, float fontSize) {
        this.complexUnitSp = complexUnitSp;
        this.fontSize = fontSize;
    }

    public CharSequence getText() {
        StringBuilder sb = new StringBuilder();
        if (this.lines.size() > 0) {
            sb.append(this.lines.get(0));
            for (int i = 1; i < this.lines.size(); i++) {
                sb.append(Document.LINE_SEPARATOR_UNIX);
                sb.append(this.lines.get(i));
            }
        }
        return sb.toString();
    }

    public void setText(List<String> text) {
        this.selectedLine = -1;
        this.lines.clear();
        for (String line : text) {
            this.lines.add(line);
        }
        this.listRef.get().resetSelection();
    }

    public List<String> getTextLines() {
        return this.lines;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setCodeTheme(ColorTheme theme) {
        this.backgroundColor = theme.getBackgroundColor();
        this.gutterColor = theme.getGutterColor();
        this.lineNumberColor = theme.getLineNumberColor();
        this.dividerColor = theme.getDividerColor();
        this.currentLineColor = theme.getCurrentLineColor();
        updateVisibleRows();
    }

    public void setShowLineNumbers(boolean bShow) {
        this.bShowLineNumbers = bShow;
    }

    public void setTextChangeListener(TextWatcher textChangeListener) {
        this.textChangeListener = textChangeListener;
    }

    static class SelectionRecord {
        long time = 0;
        int position = -1;
        int selStart = 0;
        int selEnd = 0;

        SelectionRecord() {
        }
    }

    static class ViewHolder {
        View divider;
        EditTextRememberCursor et;
        TextView lineTv;
        View root;

        ViewHolder() {
        }
    }

    class MyHandler extends Handler {
        View target;

        MyHandler() {
        }

        public void setTargetView(EditText target) {
            this.target = target;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    this.target.requestFocus();
                    break;
            }
        }
    }

    private class CustomTextWatcher implements TextWatcher {
        private boolean isSetInitialText = true;
        private int position;

        public CustomTextWatcher(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (this.isSetInitialText) {
                this.isSetInitialText = false;
                return;
            }
            if (this.position < TextEditBigListAdapter.this.lines.size()) {
                TextEditBigListAdapter.this.lines.set(this.position, s.toString());
            }
            if (TextEditBigListAdapter.this.textChangeListener != null) {
                TextEditBigListAdapter.this.textChangeListener.afterTextChanged(s);
            }
        }
    }
}
