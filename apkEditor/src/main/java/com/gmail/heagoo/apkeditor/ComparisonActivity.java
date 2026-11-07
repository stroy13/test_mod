package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.common.ThemedActivity;
import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.gmail.heagoo.apkeditor.ComparisonActivity;
import com.gmail.heagoo.apkeditor.ComparisonCore;
import com.gmail.heagoo.apkeditor.ComparisonDiffDialog;
import com.gmail.heagoo.apkeditor.ComparisonPresentation;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class ComparisonActivity extends ThemedActivity implements FileSelectDialog.IFileSelection {
    public static final String EXTRA_FIRST = "comparison.first";
    public static final String EXTRA_MODE = "comparison.mode";
    public static final String EXTRA_RUN = "comparison.run";
    public static final String EXTRA_SECOND = "comparison.second";
    private EntryAdapter adapter;
    private Dialog binaryDialog;
    private volatile boolean busy;
    private volatile boolean cancelled;
    private Spinner category;
    private Button chooseLeft;
    private Button chooseRight;
    private File decodedTemp;
    private volatile boolean destroyed;
    private LinearLayout filterBar;
    private Button go;
    private String left;
    private TextView leftLabel;
    private ListView list;
    private Spinner mode;
    private ComparisonCore.Result result;
    private String right;
    private TextView rightLabel;
    private EditText search;
    private TextView status;
    private TextView summary;
    private Button swap;
    private final Runnable applySearch = new Runnable() {
        @Override
        public final void run() {
            ComparisonActivity.this.m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
        }
    };
    private final List<ComparisonCore.Entry> visible = new ArrayList();

    public int dp(float n) {
        return (int) ((getResources().getDisplayMetrics().density * n) + 0.5f);
    }

    @Override
    protected void onCreate(Bundle state) {
        String stringExtra;
        super.onCreate(state);
        setTitle("Сравнение");
        if (state != null) {
            this.left = state.getString("left");
            stringExtra = state.getString("right");
        } else {
            this.left = getIntent().getStringExtra(EXTRA_FIRST);
            stringExtra = getIntent().getStringExtra(EXTRA_SECOND);
        }
        this.right = stringExtra;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(10.0f), dp(8.0f), dp(10.0f), dp(8.0f));
        setContentView(linearLayout);
        this.mode = new Spinner(this);
        String[] types = {"APK / ZIP — содержимое", "Файлы — текст или бинарные", "Папки — содержимое", "APK — декодированные XML и smali", "DEX / APK — классы smali", "ARSC / APK — записи ресурсов", "AXML / XML / APK — содержимое"};
        ArrayAdapter<String> modes = new ArrayAdapter<>(this, R.layout.simple_spinner_dropdown_item, types);
        this.mode.setAdapter((SpinnerAdapter) modes);
        linearLayout.addView(this.mode);
        LinearLayout selectors = new LinearLayout(this);
        this.chooseLeft = new Button(this);
        this.chooseLeft.setText("Первый");
        this.chooseRight = new Button(this);
        this.chooseRight.setText("Второй");
        selectors.addView(this.chooseLeft, new LinearLayout.LayoutParams(0, -2, 1.0f));
        selectors.addView(this.chooseRight, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(selectors);
        this.leftLabel = new TextView(this);
        this.rightLabel = new TextView(this);
        this.leftLabel.setTextSize(12.0f);
        this.rightLabel.setTextSize(12.0f);
        linearLayout.addView(this.leftLabel);
        linearLayout.addView(this.rightLabel);
        LinearLayout actions = new LinearLayout(this);
        this.go = new Button(this);
        this.go.setText("Сравнить");
        this.swap = new Button(this);
        this.swap.setText("Поменять ↕");
        this.swap.setTextSize(11.0f);
        Button help = new Button(this);
        help.setText("?");
        help.setMinWidth(0);
        help.setMinimumWidth(0);
        actions.addView(this.go, new LinearLayout.LayoutParams(0, -2, 1.0f));
        actions.addView(this.swap, new LinearLayout.LayoutParams(dp(112.0f), -2));
        actions.addView(help, new LinearLayout.LayoutParams(dp(48.0f), -2));
        linearLayout.addView(actions);
        this.summary = new TextView(this);
        this.summary.setTextSize(12.0f);
        this.summary.setTypeface(null, 1);
        this.summary.setPadding(dp(2.0f), dp(5.0f), dp(2.0f), dp(2.0f));
        linearLayout.addView(this.summary);
        this.filterBar = new LinearLayout(this);
        this.category = new Spinner(this);
        String[] choices = {"Все отличия", "Изменено", "Добавлено", "Удалено", "Без изменений", "Все файлы"};
        this.category.setAdapter((SpinnerAdapter) new ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, choices));
        this.search = new EditText(this);
        this.search.setSingleLine(true);
        this.search.setTextSize(12.0f);
        this.search.setHint("Поиск по пути");
        this.search.setInputType(1);
        this.filterBar.addView(this.category, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.filterBar.addView(this.search, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.filterBar.setVisibility(8);
        linearLayout.addView(this.filterBar);
        this.status = new TextView(this);
        this.status.setTextSize(12.0f);
        this.status.setPadding(dp(2.0f), dp(3.0f), dp(2.0f), dp(3.0f));
        linearLayout.addView(this.status);
        this.list = new ListView(this);
        this.list.setDividerHeight(dp(4.0f));
        this.adapter = new EntryAdapter();
        this.list.setAdapter((ListAdapter) this.adapter);
        linearLayout.addView(this.list, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        updateLabels();
        this.chooseLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m159lambda$onCreate$1$comgmailheagooapkeditorComparisonActivity(view);
            }
        });
        this.chooseRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m160lambda$onCreate$2$comgmailheagooapkeditorComparisonActivity(view);
            }
        });
        this.go.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m161lambda$onCreate$3$comgmailheagooapkeditorComparisonActivity(view);
            }
        });
        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                ComparisonActivity.this.m162lambda$onCreate$4$comgmailheagooapkeditorComparisonActivity(adapterView, view, i, j);
            }
        });
        this.swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m163lambda$onCreate$5$comgmailheagooapkeditorComparisonActivity(view);
            }
        });
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m164lambda$onCreate$6$comgmailheagooapkeditorComparisonActivity(view);
            }
        });
        this.category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                if (ComparisonActivity.this.adapter != null) {
                    ComparisonActivity.this.m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });
        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
            }

            @Override
            public void beforeTextChanged(CharSequence t, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence t, int start, int before, int count) {
                ComparisonActivity.this.search.removeCallbacks(ComparisonActivity.this.applySearch);
                ComparisonActivity.this.search.postDelayed(ComparisonActivity.this.applySearch, 180L);
            }
        });
        int initial = state != null ? state.getInt("mode", 0) : getIntent().getIntExtra(EXTRA_MODE, 0);
        if (initial < 0 || initial >= types.length) {
            initial = 0;
        }
        int requested = initial;
        boolean auto = state == null && getIntent().getBooleanExtra(EXTRA_RUN, false) && this.left != null && this.right != null;
        this.mode.setSelection(requested, false);
        this.mode.setOnItemSelectedListener(new AnonymousClass3(requested, auto));
    }

    void m159lambda$onCreate$1$comgmailheagooapkeditorComparisonActivity(View v) {
        choose("left");
    }

    void m160lambda$onCreate$2$comgmailheagooapkeditorComparisonActivity(View v) {
        choose("right");
    }

    void m161lambda$onCreate$3$comgmailheagooapkeditorComparisonActivity(View v) {
        if (!this.busy) {
            runComparison();
            return;
        }
        this.cancelled = true;
        this.go.setEnabled(false);
        this.status.setText("Отмена…");
    }

    void m162lambda$onCreate$4$comgmailheagooapkeditorComparisonActivity(AdapterView p, View v, int pos, long id) {
        open(this.visible.get(pos));
    }

    void m163lambda$onCreate$5$comgmailheagooapkeditorComparisonActivity(View v) {
        if (this.busy) {
            return;
        }
        String tmp = this.left;
        this.left = this.right;
        this.right = tmp;
        boolean hadResult = this.result != null;
        this.result = null;
        clearTemp();
        updateLabels();
        m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
        if (hadResult) {
            runComparison();
        }
    }

    void m164lambda$onCreate$6$comgmailheagooapkeditorComparisonActivity(View v) {
        new AlertDialog.Builder(this).setTitle("Как читать сравнение").setMessage("Первый файл сравнивается со вторым.\n\nИЗМЕНЁН / ЗАМЕНЁН — путь одинаковый, содержимое различается. Размер может совпадать; причина изменения по сравнению не определяется.\nДОБАВЛЕНО — есть только во втором.\nУДАЛЕНО — было только в первом.\nБЕЗ ИЗМЕНЕНИЙ — содержимое совпадает.\n\nСопоставление выполняется по полному пути: переименование отображается как удаление и добавление. Нажмите на файл, чтобы увидеть текстовые или побайтовые отличия. Бинарный XML декодируется, если поддерживается.").setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
    }

    class AnonymousClass3 implements AdapterView.OnItemSelectedListener {
        boolean pending;
        int previous;
        final boolean val$auto;
        final int val$requested;

        AnonymousClass3(int i, boolean z) {
            this.val$requested = i;
            this.val$auto = z;
            this.previous = this.val$requested;
            this.pending = this.val$auto;
        }

        void m172xefc379ac() {
            if (ComparisonActivity.this.busy || ComparisonActivity.this.result != null) {
                return;
            }
            ComparisonActivity.this.runComparison();
        }

        @Override
        public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
            if (!this.pending || position == this.val$requested) {
                if (this.previous != position) {
                    ComparisonActivity.this.left = null;
                    ComparisonActivity.this.right = null;
                    ComparisonActivity.this.result = null;
                    ComparisonActivity.this.clearTemp();
                    ComparisonActivity.this.updateLabels();
                    ComparisonActivity.this.m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
                }
                this.previous = position;
                if (this.pending) {
                    this.pending = false;
                    ComparisonActivity.this.go.post(new Runnable() {
                        @Override
                        public final void run() {
                            ComparisonActivity.AnonymousClass3.this.m172xefc379ac();
                        }
                    });
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> p) {
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        b.putString("left", this.left);
        b.putString("right", this.right);
        b.putInt("mode", this.mode.getSelectedItemPosition());
        super.onSaveInstanceState(b);
    }

    public void updateLabels() {
        this.leftLabel.setText("Первый: " + (this.left == null ? "не выбран" : this.left));
        this.rightLabel.setText("Второй: " + (this.right != null ? this.right : "не выбран"));
    }

    private void choose(String side) {
        if (this.busy) {
            return;
        }
        boolean folder = this.mode.getSelectedItemPosition() == 2;
        new FileSelectDialog(this, this, null, side, folder ? "Выберите папку" : "Выберите файл", folder, false, false, "comparison").show();
    }

    @Override
    public boolean isInterestedFile(String name, String extra) {
        int m = this.mode.getSelectedItemPosition();
        String n = name.toLowerCase(Locale.ROOT);
        if (m == 4) {
            if (!n.endsWith(".dex") && !n.endsWith(".apk")) {
                return false;
            }
        } else if (m == 5) {
            if (!n.endsWith(".arsc") && !n.endsWith(".apk")) {
                return false;
            }
        } else if (m == 6) {
            if (!n.endsWith(".xml") && !n.endsWith(".apk")) {
                return false;
            }
        } else if ((m == 0 || m == 3) && !n.endsWith(".apk") && (m != 0 || !n.endsWith(".zip"))) {
            return false;
        }
        return true;
    }

    @Override
    public String getConfirmMessage(String path, String extra) {
        return path;
    }

    @Override
    public void fileSelectedInDialog(String path, String side, boolean edit) {
        if ("left".equals(side)) {
            this.left = path;
        } else {
            this.right = path;
        }
        this.result = null;
        clearTemp();
        updateLabels();
        m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
    }

    public void clearTemp() {
        if (this.decodedTemp != null) {
            CheckpointFiles.delete(this.decodedTemp);
            this.decodedTemp = null;
        }
    }

    private void controls(boolean enable) {
        this.go.setEnabled(enable);
        this.chooseLeft.setEnabled(enable);
        this.chooseRight.setEnabled(enable);
        this.mode.setEnabled(enable);
        this.swap.setEnabled(enable);
        this.category.setEnabled(enable);
        this.search.setEnabled(enable);
    }

    public void runComparison() {
        if (this.busy) {
            return;
        }
        if (this.left == null || this.right == null) {
            this.status.setText("Выберите оба объекта.");
            return;
        }
        final File a = new File(this.left);
        final File b = new File(this.right);
        final int selected = this.mode.getSelectedItemPosition();
        clearTemp();
        this.result = null;
        m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
        this.busy = true;
        controls(false);
        this.status.setText(selected >= 3 ? "Декодирование и сравнение. Это может занять время…" : "Сравнение файлов…");
        new Thread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m170x4d6ee272(selected, a, b);
            }
        }, "compare-files").start();
    }

    void m170x4d6ee272(int selected, File a, File b) {
        ComparisonCore.Result r = null;
        File work = null;
        String error = null;
        try {
            if (selected >= 3) {
                work = new File(getCacheDir(), "compare-" + UUID.randomUUID());
                if (!work.mkdir()) {
                    throw new IOException("Не удалось создать временную папку");
                }
                File one = new File(work, "one");
                File two = new File(work, "two");
                if (selected == 3) {
                    DecodedApkDiff.decode(this, a, one, work);
                    DecodedApkDiff.decode(this, b, two, work);
                } else if (selected == 4) {
                    DexApkComparison.decode(a, one, work);
                    DexApkComparison.decode(b, two, work);
                } else if (selected == 5) {
                    BinaryResourceComparison.arscOrApk(this, a, one, work);
                    BinaryResourceComparison.arscOrApk(this, b, two, work);
                } else {
                    BinaryResourceComparison.xmlOrApk(a, one);
                    BinaryResourceComparison.xmlOrApk(b, two);
                }
                r = ComparisonCore.compare(ComparisonCore.Kind.FOLDERS, one, two);
            } else {
                r = ComparisonCore.compare(selected == 0 ? ComparisonCore.Kind.ARCHIVES : selected == 2 ? ComparisonCore.Kind.FOLDERS : ComparisonCore.Kind.FILES, a, b);
            }
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        }
        final ComparisonCore.Result value = r;
        final File temp = work;
        final String failure = error;
        runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m169xd7f4bc31(temp, failure, value);
            }
        });
    }

    void m169xd7f4bc31(File temp, String failure, ComparisonCore.Result value) {
        this.busy = false;
        if (this.destroyed) {
            if (temp != null) {
                CheckpointFiles.delete(temp);
                return;
            }
            return;
        }
        controls(true);
        if (failure != null) {
            if (temp != null) {
                CheckpointFiles.delete(temp);
            }
            this.status.setText("Ошибка: " + failure);
        } else {
            this.decodedTemp = temp;
            this.result = value;
            m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
        }
    }

    private void decodeDex(File dex, File out) throws Exception {
        if (!dex.isFile() || dex.length() > 536870912) {
            throw new IOException("DEX отсутствует или больше 512 МБ");
        }
        try {
            Object decoder = Class.forName("com.gmail.heagoo.apkeditor.pro.DexDecoder").getConstructor(String.class).newInstance(dex.getAbsolutePath());
            decoder.getClass().getMethod("dex2smali", String.class).invoke(decoder, out.getAbsolutePath());
        } catch (InvocationTargetException e) {
            throw new IOException("Не удалось разобрать DEX: " + e.getCause(), e.getCause());
        }
    }

    public void m158lambda$new$0$comgmailheagooapkeditorComparisonActivity() {
        this.visible.clear();
        if (this.result != null) {
            int choice = this.category.getSelectedItemPosition();
            if (choice < 0 || choice >= ComparisonPresentation.Filter.values().length) {
                choice = 0;
            }
            ComparisonPresentation.Filter selected = ComparisonPresentation.Filter.values()[choice];
            this.visible.addAll(ComparisonPresentation.filter(this.result.entries, selected, this.search.getText().toString()));
            int[] c = ComparisonPresentation.counts(this.result.entries);
            this.summary.setText("Изменено: " + c[0] + "   Добавлено: " + c[1] + "   Удалено: " + c[2] + "\nБез изменений: " + c[3]);
            this.filterBar.setVisibility(0);
            this.status.setText(this.visible.isEmpty() ? "Нет элементов для выбранного фильтра." : "Показано: " + this.visible.size() + ". Нажмите на файл, чтобы увидеть различия.");
        } else {
            this.summary.setText("");
            this.filterBar.setVisibility(8);
            if (!this.busy) {
                this.status.setText("Первый → второй. Исходные файлы не изменяются.");
            }
        }
        this.adapter.notifyDataSetChanged();
    }

    private static final class RowViews {
        TextView detail;
        TextView kind;
        TextView name;
        TextView path;

        private RowViews() {
        }
    }

    public int background(ComparisonCore.Status s) {
        TypedValue t = new TypedValue();
        getTheme().resolveAttribute(com.gmail.heagoo.apkeditor.base.R.attr.labSurface, t, true);
        int color = t.resourceId != 0 ? ContextCompat.getColor(this, t.resourceId) : t.data;
        boolean light = ColorUtils.calculateLuminance(color) > 0.45d;
        switch (s) {
            case ADDED:
                return light ? -2493468 : -15123918;
            case REMOVED:
                return light ? -7197 : -11982803;
            case CHANGED:
                return light ? -3639 : -12305632;
            default:
                return 0;
        }
    }

    private final class EntryAdapter extends BaseAdapter {
        private EntryAdapter() {
        }

        @Override
        public int getCount() {
            return ComparisonActivity.this.visible.size();
        }

        @Override
        public Object getItem(int p) {
            return ComparisonActivity.this.visible.get(p);
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public View getView(int p, View v, ViewGroup parent) {
            LinearLayout box;
            RowViews h;
            if (v instanceof LinearLayout) {
                box = (LinearLayout) v;
                h = (RowViews) box.getTag();
            } else {
                box = new LinearLayout(ComparisonActivity.this);
                box.setOrientation(1);
                box.setPadding(ComparisonActivity.this.dp(10.0f), ComparisonActivity.this.dp(7.0f), ComparisonActivity.this.dp(10.0f), ComparisonActivity.this.dp(7.0f));
                RowViews h2 = new RowViews();
                h2.kind = new TextView(ComparisonActivity.this);
                h2.kind.setTextSize(11.0f);
                h2.kind.setTypeface(null, 1);
                h2.name = new TextView(ComparisonActivity.this);
                h2.name.setTextSize(14.0f);
                h2.name.setTypeface(null, 1);
                h2.path = new TextView(ComparisonActivity.this);
                h2.path.setTextSize(11.0f);
                h2.detail = new TextView(ComparisonActivity.this);
                h2.detail.setTextSize(11.0f);
                box.addView(h2.kind);
                box.addView(h2.name);
                box.addView(h2.path);
                box.addView(h2.detail);
                box.setTag(h2);
                h = h2;
            }
            ComparisonCore.Entry e = (ComparisonCore.Entry) ComparisonActivity.this.visible.get(p);
            h.kind.setText(ComparisonPresentation.status(e));
            h.name.setText(ComparisonPresentation.name(e));
            h.path.setText(ComparisonPresentation.parent(e));
            h.detail.setText(ComparisonPresentation.detail(e) + (e.directory ? "" : "  •  нажмите для просмотра"));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ComparisonActivity.this.background(e.status));
            bg.setCornerRadius(ComparisonActivity.this.dp(8.0f));
            box.setBackground(bg);
            return box;
        }
    }

    private void open(final ComparisonCore.Entry entry) {
        if (this.result == null || this.busy || entry.directory) {
            return;
        }
        final ComparisonCore.Result selected = this.result;
        this.busy = true;
        this.cancelled = false;
        controls(false);
        this.go.setText("Отменить");
        this.go.setEnabled(true);
        this.status.setText("Подготовка различий…");
        new Thread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m167lambda$open$12$comgmailheagooapkeditorComparisonActivity(selected, entry);
            }
        }, "compare-entry").start();
    }

    void m167lambda$open$12$comgmailheagooapkeditorComparisonActivity(ComparisonCore.Result selected, final ComparisonCore.Entry entry) {
        ComparisonCore.Diff d;
        String ltext;
        String rtext;
        File index;
        long count;
        String error;
        boolean decodedXml;
        ComparisonCore.Diff d2 = null;
        String ltext2 = null;
        String rtext2 = null;
        File index2 = null;
        long count2 = 0;
        boolean decodedXml2 = false;
        try {
            try {
            } catch (Exception e) {
                if (!this.cancelled && !this.destroyed) {
                    ltext2 = null;
                    rtext2 = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public final void run() {
                            ComparisonActivity.this.m168lambda$open$9$comgmailheagooapkeditorComparisonActivity();
                        }
                    });
                    index2 = File.createTempFile("compare-bytes-", ".idx", getCacheDir());
                    InputStream a = ComparisonCore.openInput(selected, entry, true);
                    try {
                        InputStream b = ComparisonCore.openInput(selected, entry, false);
                        try {
                            count2 = BinaryDiffIndex.build(a, b, index2, new BooleanSupplier() {
                                @Override
                                public final boolean getAsBoolean() {
                                    return ComparisonActivity.this.m165lambda$open$10$comgmailheagooapkeditorComparisonActivity();
                                }
                            });
                            if (b != null) {
                                b.close();
                            }
                            if (a != null) {
                                a.close();
                            }
                        } finally {
                        }
                    } catch (Throwable th) {
                        if (a == null) {
                            throw th;
                        }
                        try {
                            a.close();
                            throw th;
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                            throw th;
                        }
                    }
                } else {
                    throw new InterruptedIOException("Отменено");
                }
            }
        } catch (Exception ex) {
            if (index2 != null) {
                index2.delete();
            }
            String error2 = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            d = null;
            ltext = ltext2;
            rtext = rtext2;
            index = null;
            count = 0;
            error = error2;
            decodedXml = false;
        }
        try {
            if (this.cancelled || this.destroyed) {
                throw new InterruptedIOException("Отменено");
            }
            boolean xml = ComparisonEntryXml.isBinary(selected, entry, true) || ComparisonEntryXml.isBinary(selected, entry, false);
        if (xml) {
            decodedXml2 = true;
            ltext2 = ComparisonEntryXml.read(selected, entry, true);
            rtext2 = ComparisonEntryXml.read(selected, entry, false);
        } else {
            ltext2 = ComparisonCore.readText(selected, entry, true);
            rtext2 = ComparisonCore.readText(selected, entry, false);
        }
        d2 = ComparisonCore.diff(ltext2, rtext2);
        if (this.cancelled || this.destroyed) {
            throw new InterruptedIOException("Отменено");
        }
            d = d2;
            ltext = ltext2;
            rtext = rtext2;
            index = index2;
            count = count2;
            error = null;
            decodedXml = decodedXml2;
        } catch (Exception ex) {
            d = null;
            ltext = ltext2;
            rtext = rtext2;
            index = null;
            count = 0;
            error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            decodedXml = false;
        }
        final ComparisonCore.Diff diff = d;
        final String failure = error;
        final String leftText = ltext;
        final String rightText = rtext;
        final File binary = index;
        final long changes = count;
        final boolean isDecoded = decodedXml;
        final boolean wasCancelled = this.cancelled;
        runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m166lambda$open$11$comgmailheagooapkeditorComparisonActivity(binary, wasCancelled, failure, entry, changes, diff, leftText, rightText, isDecoded);
            }
        });
    }

    void m168lambda$open$9$comgmailheagooapkeditorComparisonActivity() {
        if (this.destroyed || this.cancelled) {
            return;
        }
        this.status.setText("Поиск побайтовых различий. Крупный файл может обрабатываться несколько минут…");
    }

    boolean m165lambda$open$10$comgmailheagooapkeditorComparisonActivity() {
        return this.destroyed || this.cancelled;
    }

    void m166lambda$open$11$comgmailheagooapkeditorComparisonActivity(File binary, boolean wasCancelled, String failure, ComparisonCore.Entry entry, long changes, ComparisonCore.Diff diff, String leftText, String rightText, boolean isDecoded) {
        this.busy = false;
        if (this.destroyed) {
            if (binary != null) {
                binary.delete();
            }
            clearTemp();
            return;
        }
        controls(true);
        this.go.setText("Сравнить");
        m158lambda$new$0$comgmailheagooapkeditorComparisonActivity();
        if (wasCancelled || this.cancelled) {
            if (binary != null) {
                binary.delete();
            }
            this.status.setText("Просмотр отменён.");
        } else {
            if (failure != null) {
                new AlertDialog.Builder(this).setTitle(entry.name).setMessage("Не удалось открыть различия: " + failure).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                return;
            }
            if (binary == null) {
                showDiff(entry, diff, leftText, rightText, isDecoded);
                return;
            }
            try {
                this.binaryDialog = new BinaryDiffDialog(this, entry.name, binary, changes).dialog();
            } catch (Exception ex) {
                binary.delete();
                new AlertDialog.Builder(this).setMessage("Не удалось открыть список: " + ex.getMessage()).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }
    }

    private void showDiff(final ComparisonCore.Entry entry, ComparisonCore.Diff diff, final String leftText, final String rightText, boolean decodedXml) {
        boolean editable = !decodedXml && (this.mode.getSelectedItemPosition() == 1 || this.mode.getSelectedItemPosition() == 2);
        new ComparisonDiffDialog(this, entry, diff, editable && leftText != null, editable && rightText != null, new ComparisonDiffDialog.Edit() {
            @Override
            public final void open(Dialog dialog, boolean z) {
                ComparisonActivity.this.m171lambda$showDiff$13$comgmailheagooapkeditorComparisonActivity(entry, leftText, rightText, dialog, z);
            }
        });
    }

    void m171lambda$showDiff$13$comgmailheagooapkeditorComparisonActivity(ComparisonCore.Entry entry, String leftText, String rightText, Dialog dialog, boolean first) {
        editTextFile(dialog, entry, first, first ? leftText : rightText);
    }

    private void editTextFile(final Dialog comparison, ComparisonCore.Entry entry, boolean first, final String original) {
        if (original != null && this.result != null) {
            try {
                final File source = ComparisonTextSave.resolve(this.result, entry, first);
                final EditText field = new EditText(this);
                field.setTypeface(Typeface.MONOSPACE);
                field.setTextSize(13.0f);
                field.setGravity(48);
                field.setSingleLine(false);
                field.setText(original);
                final AlertDialog editor = new AlertDialog.Builder(this).setTitle("Редактирование: " + source.getName()).setView(field).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Сохранить", (DialogInterface.OnClickListener) null).create();
                editor.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public final void onShow(DialogInterface dialogInterface) {
                        ComparisonActivity.this.m157xd16611e2(editor, field, source, original, comparison, dialogInterface);
                    }
                });
                editor.show();
            } catch (Exception ex) {
                new AlertDialog.Builder(this).setMessage(ex.getMessage()).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }
    }

    void m153xfb7d78de(String failure, AlertDialog editor, Dialog comparison) {
        if (this.destroyed) {
            return;
        }
        if (failure != null) {
            editor.getButton(-1).setEnabled(true);
            new AlertDialog.Builder(this).setMessage("Не сохранено: " + failure).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
        } else {
            editor.dismiss();
            comparison.dismiss();
            runComparison();
        }
    }

    void m154x70f79f1f(File source, String original, String replacement, final AlertDialog editor, final Dialog comparison) {
        String error = null;
        try {
            ComparisonTextSave.save(source, original, replacement);
        } catch (Exception ex) {
            error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        }
        final String failure = error;
        runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m153xfb7d78de(failure, editor, comparison);
            }
        });
    }

    void m155xe671c560(final AlertDialog editor, final File source, final String original, final String replacement, final Dialog comparison, DialogInterface dlg, int which) {
        editor.getButton(-1).setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public final void run() {
                ComparisonActivity.this.m154x70f79f1f(source, original, replacement, editor, comparison);
            }
        }, "comparison-save").start();
    }

    void m156x5bebeba1(EditText field, final File source, final AlertDialog editor, final String original, final Dialog comparison, View v) {
        final String replacement = field.getText().toString();
        new AlertDialog.Builder(this).setMessage("Сохранить изменения в исходный файл?\n" + source).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                ComparisonActivity.this.m155xe671c560(editor, source, original, replacement, comparison, dialogInterface, i);
            }
        }).show();
    }

    void m157xd16611e2(final AlertDialog editor, final EditText field, final File source, final String original, final Dialog comparison, DialogInterface d) {
        editor.getButton(-1).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ComparisonActivity.this.m156x5bebeba1(field, source, editor, original, comparison, view);
            }
        });
    }

    @Override
    protected void onDestroy() {
        this.destroyed = true;
        if (this.search != null) {
            this.search.removeCallbacks(this.applySearch);
        }
        if (this.binaryDialog != null && this.binaryDialog.isShowing()) {
            this.binaryDialog.dismiss();
        }
        if (!this.busy) {
            clearTemp();
        }
        super.onDestroy();
    }
}
