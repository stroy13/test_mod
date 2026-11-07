package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.gmail.heagoo.apkeditor.BinaryDiffIndex;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.neweditor.ParserRule;
import com.gmail.heagoo.neweditor.Token;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BinaryDiffDialog {
    private final Activity activity;
    private final BaseAdapter adapter;
    private final long count;
    private final Dialog dialog;
    private final RandomAccessFile file;
    private int green;
    private final File index;
    private final Button next;
    private long page;
    private final EditText pageInput;
    private final TextView pageLabel;
    private final long pages;
    private final Button previous;
    private int red;
    private final List<BinaryDiffIndex.Row> rows = new ArrayList();

    public int dp(float d) {
        return (int) ((this.activity.getResources().getDisplayMetrics().density * d) + 0.5f);
    }

    public Dialog dialog() {
        return this.dialog;
    }

    public BinaryDiffDialog(Activity a, String name, File f, long c) throws IOException {
        this.activity = a;
        this.index = f;
        this.count = c;
        this.pages = Math.max(1L, ((this.count + 128) - 1) / 128);
        this.file = new RandomAccessFile(f, "r");
        this.dialog = new Dialog(a);
        TypedValue t = new TypedValue();
        a.getTheme().resolveAttribute(R.attr.labSurface, t, true);
        int color = t.resourceId != 0 ? ContextCompat.getColor(a, t.resourceId) : t.data;
        boolean light = ColorUtils.calculateLuminance(color) > 0.45d;
        this.red = light ? -5692627 : -25179;
        this.green = light ? -15569086 : -7476814;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(1);
        box.setPadding(dp(8.0f), dp(8.0f), dp(8.0f), dp(8.0f));
        TextView title = new TextView(a);
        title.setText(name);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        title.setTextSize(14.0f);
        box.addView(title);
        TextView note = new TextView(a);
        note.setText("Байты по одинаковым смещениям: − первый, + второй. Вставка может сдвинуть последующие байты.");
        note.setTextSize(11.0f);
        box.addView(note);
        LinearLayout nav = new LinearLayout(a);
        this.previous = button("‹");
        this.next = button("›");
        this.pageLabel = new TextView(a);
        this.pageLabel.setGravity(17);
        this.pageLabel.setTextSize(11.0f);
        this.pageInput = new EditText(a);
        this.pageInput.setSingleLine(true);
        this.pageInput.setTextSize(12.0f);
        this.pageInput.setHint("Стр.");
        this.pageInput.setInputType(2);
        Button jump = button("Перейти");
        Button close = button("✕");
        nav.addView(this.previous, new LinearLayout.LayoutParams(0, dp(44.0f), 0.7f));
        nav.addView(this.pageLabel, new LinearLayout.LayoutParams(0, dp(44.0f), 1.5f));
        nav.addView(this.next, new LinearLayout.LayoutParams(0, dp(44.0f), 0.7f));
        nav.addView(this.pageInput, new LinearLayout.LayoutParams(0, dp(44.0f), 0.9f));
        nav.addView(jump, new LinearLayout.LayoutParams(0, dp(44.0f), 1.2f));
        nav.addView(close, new LinearLayout.LayoutParams(0, dp(44.0f), 0.7f));
        box.addView(nav);
        final ListView list = new ListView(a);
        list.setDividerHeight(dp(1.0f));
        this.adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return BinaryDiffDialog.this.rows.size();
            }

            @Override
            public Object getItem(int p) {
                return BinaryDiffDialog.this.rows.get(p);
            }

            @Override
            public long getItemId(int p) {
                return p;
            }

            @Override
            public View getView(int p, View convert, ViewGroup parent) {
                TextView v = convert instanceof TextView ? (TextView) convert : new TextView(BinaryDiffDialog.this.activity);
                v.setTypeface(Typeface.MONOSPACE);
                v.setTextSize(10.0f);
                v.setPadding(BinaryDiffDialog.this.dp(6.0f), BinaryDiffDialog.this.dp(5.0f), BinaryDiffDialog.this.dp(6.0f), BinaryDiffDialog.this.dp(5.0f));
                v.setText(BinaryDiffDialog.this.render((BinaryDiffIndex.Row) BinaryDiffDialog.this.rows.get(p)));
                return v;
            }
        };
        list.setAdapter((ListAdapter) this.adapter);
        box.addView(list, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                BinaryDiffDialog.this.m126lambda$new$0$comgmailheagooapkeditorBinaryDiffDialog(list, view);
            }
        });
        this.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                BinaryDiffDialog.this.m127lambda$new$1$comgmailheagooapkeditorBinaryDiffDialog(list, view);
            }
        });
        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                BinaryDiffDialog.this.m128lambda$new$2$comgmailheagooapkeditorBinaryDiffDialog(list, view);
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                BinaryDiffDialog.this.m129lambda$new$3$comgmailheagooapkeditorBinaryDiffDialog(view);
            }
        });
        this.dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                BinaryDiffDialog.this.m130lambda$new$4$comgmailheagooapkeditorBinaryDiffDialog(dialogInterface);
            }
        });
        this.dialog.setContentView(box);
        load();
        this.dialog.show();
        if (this.dialog.getWindow() != null) {
            this.dialog.getWindow().setLayout(-1, -1);
        }
    }

    void m126lambda$new$0$comgmailheagooapkeditorBinaryDiffDialog(ListView list, View v) {
        if (this.page > 0) {
            this.page--;
            load();
            list.setSelection(0);
        }
    }

    void m127lambda$new$1$comgmailheagooapkeditorBinaryDiffDialog(ListView list, View v) {
        if (this.page + 1 < this.pages) {
            this.page++;
            load();
            list.setSelection(0);
        }
    }

    void m128lambda$new$2$comgmailheagooapkeditorBinaryDiffDialog(ListView list, View v) {
        try {
            long p = Long.parseLong(this.pageInput.getText().toString());
            if (p < 1 || p > this.pages) {
                throw new NumberFormatException();
            }
            this.page = p - 1;
            load();
            list.setSelection(0);
        } catch (NumberFormatException e) {
            this.pageInput.setError("1…" + this.pages);
        }
    }

    void m129lambda$new$3$comgmailheagooapkeditorBinaryDiffDialog(View v) {
        this.dialog.dismiss();
    }

    void m130lambda$new$4$comgmailheagooapkeditorBinaryDiffDialog(DialogInterface d) {
        try {
            this.file.close();
        } catch (IOException e) {
        }
        this.index.delete();
    }

    private Button button(String text) {
        Button b = new Button(this.activity);
        b.setText(text);
        b.setTextSize(10.0f);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private void load() {
        try {
            this.rows.clear();
            this.rows.addAll(BinaryDiffIndex.readPage(this.file, this.page * 128, BinaryDiffIndex.PAGE_ROWS));
            this.adapter.notifyDataSetChanged();
            this.pageLabel.setText(this.count == 0 ? "Нет отличий" : (this.page + 1) + " / " + this.pages + "\nСтрок: " + this.count);
            this.previous.setEnabled(this.page > 0);
            this.next.setEnabled(this.page + 1 < this.pages);
        } catch (IOException e) {
            new AlertDialog.Builder(this.activity).setMessage("Не удалось прочитать страницу: " + e.getMessage()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }

    public CharSequence render(BinaryDiffIndex.Row row) {
        SpannableStringBuilder s = new SpannableStringBuilder(String.format(Locale.ROOT, "%08X\n− ", Long.valueOf(row.offset)));
        append(s, row, row.left, this.red);
        s.append("\n+ ");
        append(s, row, row.right, this.green);
        return s;
    }

    private void append(SpannableStringBuilder s, BinaryDiffIndex.Row row, byte[] bytes, int color) {
        char[] h = "0123456789ABCDEF".toCharArray();
        int size = Math.max(row.left.length, row.right.length);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                s.append(' ');
            }
            int start = s.length();
            if (i >= bytes.length) {
                s.append("--");
            } else {
                s.append(h[(bytes[i] & ParserRule.MATCH_TYPE_CONTEXT) >>> 4]);
                s.append(h[bytes[i] & Token.LITERAL3]);
            }
            if (row.differs(i)) {
                s.setSpan(new ForegroundColorSpan(color), start, s.length(), 33);
            }
        }
    }
}
