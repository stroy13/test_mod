package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.gmail.heagoo.apkeditor.ComparisonCore;
import com.gmail.heagoo.apkeditor.ComparisonDiffDialog;
import com.gmail.heagoo.apkeditor.base.R;
import java.util.ArrayList;
import java.util.List;

public final class ComparisonDiffDialog {
    private int active;
    private final Activity activity;
    private LinearLayout container;
    private CheckBox context;
    private final Dialog dialog;
    private final ComparisonCore.Diff diff;
    private boolean inSync;
    private int longest;
    private CheckBox sync;
    private boolean vertical;
    private final List<ComparisonCore.Row> rows = new ArrayList();
    private final Pane[] panes = new Pane[2];
    private float fontSize = 12.0f;

    public interface Edit {
        void open(Dialog dialog, boolean z);
    }

    public int dp(float n) {
        return (int) ((this.activity.getResources().getDisplayMetrics().density * n) + 0.5f);
    }

    public int bg(ComparisonCore.Status status) {
        TypedValue t = new TypedValue();
        this.activity.getTheme().resolveAttribute(R.attr.labSurface, t, true);
        int c = t.resourceId != 0 ? ContextCompat.getColor(this.activity, t.resourceId) : t.data;
        boolean light = ColorUtils.calculateLuminance(c) > 0.45d;
        switch (status) {
            case ADDED:
                return light ? -2493468 : -15123918;
            case REMOVED:
                return light ? -7197 : -11982803;
            default:
                return 0;
        }
    }

    public ComparisonDiffDialog(Activity a, ComparisonCore.Entry entry, ComparisonCore.Diff d, boolean canLeft, boolean canRight, final Edit edit) {
        int i;
        this.activity = a;
        this.diff = d;
        this.rows.addAll(d.rows);
        for (ComparisonCore.Row r : d.rows) {
            this.longest = Math.max(this.longest, Math.max(r.left.length(), r.right.length()));
        }
        this.dialog = new Dialog(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(1);
        box.setPadding(dp(6.0f), dp(6.0f), dp(6.0f), dp(6.0f));
        TextView title = new TextView(a);
        title.setText(entry.name);
        title.setTextSize(13.0f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        box.addView(title);
        LinearLayout nav = new LinearLayout(a);
        Button previous = button("‹");
        Button next = button("›");
        Button smaller = button("A−");
        Button bigger = button("A+");
        Button rotate = button("↔/↕");
        Button close = button("✕");
        int i2 = 0;
        Button[] buttonArr = {previous, next, smaller, bigger, rotate, close};
        while (true) {
            Button[] buttonArr2 = buttonArr;
            if (i2 >= 6) {
                break;
            }
            Button b = buttonArr2[i2];
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(42.0f), 1.0f));
            i2++;
            buttonArr = buttonArr2;
            close = close;
        }
        Button close2 = close;
        box.addView(nav);
        LinearLayout flags = new LinearLayout(a);
        this.sync = new CheckBox(a);
        this.sync.setText("Синхр.");
        this.sync.setChecked(true);
        this.context = new CheckBox(a);
        this.context.setText("Только отличия");
        Button edit1 = button("Править 1");
        Button edit2 = button("Править 2");
        edit1.setEnabled(canLeft);
        edit2.setEnabled(canRight);
        this.sync.setTextSize(11.0f);
        this.context.setTextSize(11.0f);
        flags.addView(this.sync, new LinearLayout.LayoutParams(0, -2, 1.0f));
        flags.addView(this.context, new LinearLayout.LayoutParams(0, -2, 1.0f));
        box.addView(flags);
        if (canLeft || canRight) {
            LinearLayout editBar = new LinearLayout(a);
            i = 0;
            editBar.addView(edit1, new LinearLayout.LayoutParams(0, dp(40.0f), 1.0f));
            editBar.addView(edit2, new LinearLayout.LayoutParams(0, dp(40.0f), 1.0f));
            box.addView(editBar);
        } else {
            i = 0;
        }
        this.container = new LinearLayout(a);
        this.container.setOrientation(i);
        this.panes[i] = new Pane(i);
        this.panes[1] = new Pane(1);
        this.container.addView(this.panes[i].view, new LinearLayout.LayoutParams(i, -1, 1.0f));
        this.container.addView(this.panes[1].view, new LinearLayout.LayoutParams(i, -1, 1.0f));
        box.addView(this.container, new LinearLayout.LayoutParams(-1, i, 1.0f));
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m181lambda$new$0$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m182lambda$new$1$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        smaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m183lambda$new$2$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        bigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m184lambda$new$3$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m185lambda$new$4$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        close2.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m186lambda$new$5$comgmailheagooapkeditorComparisonDiffDialog(view);
            }
        });
        edit1.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m187lambda$new$6$comgmailheagooapkeditorComparisonDiffDialog(edit, view);
            }
        });
        edit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonDiffDialog.this.m188lambda$new$7$comgmailheagooapkeditorComparisonDiffDialog(edit, view);
            }
        });
        this.sync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ComparisonDiffDialog.this.m189lambda$new$8$comgmailheagooapkeditorComparisonDiffDialog(compoundButton, z);
            }
        });
        this.context.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ComparisonDiffDialog.this.m190lambda$new$9$comgmailheagooapkeditorComparisonDiffDialog(compoundButton, z);
            }
        });
        this.dialog.setContentView(box);
        refresh();
        this.dialog.show();
        if (this.dialog.getWindow() != null) {
            this.dialog.getWindow().setLayout(-1, -1);
        }
    }

    void m181lambda$new$0$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        navigate(-1);
    }

    void m182lambda$new$1$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        navigate(1);
    }

    void m183lambda$new$2$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        setFont(this.fontSize - 1.0f);
    }

    void m184lambda$new$3$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        setFont(this.fontSize + 1.0f);
    }

    void m185lambda$new$4$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        rotate();
    }

    void m186lambda$new$5$comgmailheagooapkeditorComparisonDiffDialog(View v) {
        this.dialog.dismiss();
    }

    void m187lambda$new$6$comgmailheagooapkeditorComparisonDiffDialog(Edit edit, View v) {
        edit.open(this.dialog, true);
    }

    void m188lambda$new$7$comgmailheagooapkeditorComparisonDiffDialog(Edit edit, View v) {
        edit.open(this.dialog, false);
    }

    void m189lambda$new$8$comgmailheagooapkeditorComparisonDiffDialog(CompoundButton b, boolean c) {
        if (c) {
            align(this.panes[this.active]);
        }
    }

    void m190lambda$new$9$comgmailheagooapkeditorComparisonDiffDialog(CompoundButton b, boolean c) {
        this.rows.clear();
        List<ComparisonCore.Row> list = this.rows;
        List<ComparisonCore.Row> list2 = this.diff.rows;
        if (c) {
            list2 = DiffRows.withContext(list2, 3);
        }
        list.addAll(list2);
        refresh();
        this.panes[0].list.setSelection(0);
        this.panes[1].list.setSelection(0);
    }

    private Button button(String text) {
        Button b = new Button(this.activity);
        b.setText(text);
        b.setTextSize(11.0f);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(2.0f), 0, dp(2.0f), 0);
        b.setSingleLine(true);
        return b;
    }

    private void rotate() {
        this.vertical = !this.vertical;
        this.container.setOrientation(this.vertical ? 1 : 0);
        for (Pane pane : this.panes) {
            pane.view.setLayoutParams(this.vertical ? new LinearLayout.LayoutParams(-1, 0, 1.0f) : new LinearLayout.LayoutParams(0, -1, 1.0f));
        }
    }

    public void setFont(float size) {
        float n = Math.min(24.0f, Math.max(8.0f, size));
        if (Math.abs(n - this.fontSize) < 0.25f) {
            return;
        }
        this.fontSize = n;
        refresh();
    }

    private void refresh() {
        TextView measure = new TextView(this.activity);
        measure.setTypeface(Typeface.MONOSPACE);
        measure.setTextSize(this.fontSize);
        int width = (int) Math.min(dp(8192.0f), Math.max(dp(1000.0f), (this.longest + 12) * measure.getPaint().measureText("M") * 1.5d));
        for (Pane p : this.panes) {
            p.list.setLayoutParams(new FrameLayout.LayoutParams(width, -1));
            p.adapter.notifyDataSetChanged();
        }
    }

    public void align(Pane source) {
        Pane other;
        View child;
        if (!this.sync.isChecked() || this.inSync || (other = this.panes[1 - source.side]) == null || (child = source.list.getChildAt(0)) == null) {
            return;
        }
        int pos = source.list.getFirstVisiblePosition();
        int top = child.getTop();
        View o = other.list.getChildAt(0);
        if (o == null || other.list.getFirstVisiblePosition() != pos || Math.abs(o.getTop() - top) > 1) {
            this.inSync = true;
            other.list.setSelectionFromTop(pos, top);
            this.inSync = false;
        }
    }

    private void navigate(int direction) {
        Pane source = this.panes[this.active];
        int at = source.list.getFirstVisiblePosition();
        int i = at + direction;
        while (i >= 0 && i < this.rows.size()) {
            if (this.rows.get(i).status != ComparisonCore.Status.SAME) {
                source.list.setSelection(i);
                if (this.sync.isChecked()) {
                    this.panes[1 - this.active].list.setSelection(i);
                    return;
                }
                return;
            }
            i += direction;
        }
    }

    final class Pane {
        final BaseAdapter adapter;
        final ListView list;
        final int side;
        final LinearLayout view;

        Pane(int n) {
            this.side = n;
            this.view = new LinearLayout(ComparisonDiffDialog.this.activity);
            this.view.setOrientation(1);
            TextView head = new TextView(ComparisonDiffDialog.this.activity);
            head.setText(n == 0 ? "Первый — было" : "Второй — стало");
            head.setTextSize(12.0f);
            head.setPadding(ComparisonDiffDialog.this.dp(4.0f), ComparisonDiffDialog.this.dp(2.0f), ComparisonDiffDialog.this.dp(4.0f), ComparisonDiffDialog.this.dp(2.0f));
            this.view.addView(head);
            HorizontalScrollView horizontal = new HorizontalScrollView(ComparisonDiffDialog.this.activity);
            horizontal.setFillViewport(true);
            horizontal.setHorizontalScrollBarEnabled(true);
            this.view.addView(horizontal, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            this.list = new ListView(ComparisonDiffDialog.this.activity);
            this.list.setDividerHeight(0);
            horizontal.addView(this.list, new FrameLayout.LayoutParams(ComparisonDiffDialog.this.dp(1000.0f), -1));
            this.adapter = new BaseAdapter() {
                @Override
                public int getCount() {
                    return ComparisonDiffDialog.this.rows.size();
                }

                @Override
                public Object getItem(int p) {
                    return ComparisonDiffDialog.this.rows.get(p);
                }

                @Override
                public long getItemId(int p) {
                    return p;
                }

                @Override
                public View getView(int p, View convert, ViewGroup parent) {
                    TextView t = convert instanceof TextView ? (TextView) convert : new TextView(ComparisonDiffDialog.this.activity);
                    t.setTypeface(Typeface.MONOSPACE);
                    t.setTextSize(ComparisonDiffDialog.this.fontSize);
                    t.setPadding(ComparisonDiffDialog.this.dp(4.0f), ComparisonDiffDialog.this.dp(3.0f), ComparisonDiffDialog.this.dp(4.0f), ComparisonDiffDialog.this.dp(3.0f));
                    t.setSingleLine(true);
                    t.setEllipsize(null);
                    t.setHeight(t.getPaint().getFontMetricsInt(null) + ComparisonDiffDialog.this.dp(6.0f));
                    ComparisonCore.Row row = (ComparisonCore.Row) ComparisonDiffDialog.this.rows.get(p);
                    int number = Pane.this.side == 0 ? row.leftNumber : row.rightNumber;
                    String text = Pane.this.side == 0 ? row.left : row.right;
                    t.setText((number == 0 ? "" : number + "  ") + text);
                    ComparisonCore.Status color = row.status == ComparisonCore.Status.CHANGED ? Pane.this.side == 0 ? ComparisonCore.Status.REMOVED : ComparisonCore.Status.ADDED : row.status;
                    t.setBackgroundColor(number == 0 ? 0 : ComparisonDiffDialog.this.bg(color));
                    return t;
                }
            };
            this.list.setAdapter((ListAdapter) this.adapter);
            this.list.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScroll(AbsListView v, int first, int visible, int total) {
                    ComparisonDiffDialog.this.align(Pane.this);
                }

                @Override
                public void onScrollStateChanged(AbsListView v, int state) {
                }
            });
            final ScaleGestureDetector scale = new ScaleGestureDetector(ComparisonDiffDialog.this.activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    ComparisonDiffDialog.this.setFont(ComparisonDiffDialog.this.fontSize * detector.getScaleFactor());
                    return true;
                }
            });
            this.list.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    return ComparisonDiffDialog.Pane.this.m191x36231078(scale, view, motionEvent);
                }
            });
        }

        boolean m191x36231078(ScaleGestureDetector scale, View v, MotionEvent e) {
            ComparisonDiffDialog.this.active = this.side;
            scale.onTouchEvent(e);
            return scale.isInProgress();
        }
    }
}
