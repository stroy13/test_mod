package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.PatchLogBuffer;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.ManifestInfo;
import com.gmail.heagoo.apkeditor.ce.ManifestParser;
import com.gmail.heagoo.apkeditor.patch.IPatchContext;
import com.gmail.heagoo.apkeditor.patch.PatchExecutor;
import com.gmail.heagoo.apkeditor.patch.PatchPreflight;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.Document;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchDialog extends Dialog implements View.OnClickListener, IPatchContext {
    private static final String LAST_LOG_FILE = "last_patch_log.txt";
    private WeakReference<ApkInfoActivity> activityRef;
    private TextView curPatchTv;
    private final AtomicInteger errorCount;
    private String exampleDir;
    private Map<String, String> globalVariableValues;
    private volatile PatchLogBuffer logBuffer;
    private final Handler logHandler;
    private View logLayout;
    private volatile String logSaveError;
    private TextView logTv;
    private final AtomicBoolean logUpdatePending;
    private ManifestInfo manifestInfo;
    private PatchExecutor patchExecutor;
    private String patchPath;
    private TextView patchPathTv;
    private volatile boolean patchRunning;
    private TextView saveExamplesTv;
    private Button selectApplyBtn;
    private WebView webView;

    /* JADX WARN: Multi-variable type inference failed */
    public PatchDialog(ApkInfoActivity apkInfoActivity) {
        super(apkInfoActivity);
        this.exampleDir = null;
        this.patchPath = null;
        this.errorCount = new AtomicInteger();
        this.logHandler = new Handler(Looper.getMainLooper());
        this.logUpdatePending = new AtomicBoolean();
        this.globalVariableValues = new HashMap();
        requestWindowFeature(1);
        this.activityRef = new WeakReference<>(apkInfoActivity);
        init(apkInfoActivity);
        DisplayMetrics metrics = apkInfoActivity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((width * 7) / 8, -2);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void init(ApkInfoActivity apkInfoActivity) {
        int resId = R.layout.dlg_patch;
        View view = LayoutInflater.from(apkInfoActivity).inflate(resId, (ViewGroup) null);
        this.curPatchTv = (TextView) view.findViewById(R.id.tv_curpatch);
        this.curPatchTv.setOnClickListener(this);
        this.patchPathTv = (TextView) view.findViewById(R.id.tv_patch_path);
        Button closeBtn = (Button) view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        this.selectApplyBtn = (Button) view.findViewById(R.id.btn_select_apply);
        this.selectApplyBtn.setOnClickListener(this);
        view.findViewById(R.id.btn_patch_preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                PatchDialog.this.m248lambda$init$0$comgmailheagooapkeditorPatchDialog(view2);
            }
        });
        view.findViewById(R.id.btn_patch_log_copy).setOnClickListener(this);
        view.findViewById(R.id.btn_patch_log_last).setOnClickListener(this);
        view.findViewById(R.id.btn_patch_log_share).setOnClickListener(this);
        this.saveExamplesTv = (TextView) view.findViewById(R.id.tv_save_patches);
        this.saveExamplesTv.setOnClickListener(this);
        this.webView = (WebView) view.findViewById(R.id.web_instructions);
        ThemedDocument.load(this.webView, R.raw.about_patch);
        this.logLayout = view.findViewById(R.id.log_layout);
        this.logTv = (TextView) view.findViewById(R.id.tv_patchlog);
        setContentView(view);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
    }

    void m248lambda$init$0$comgmailheagooapkeditorPatchDialog(View v) {
        previewPatch();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dismiss();
            return;
        }
        if (id == R.id.btn_patch_log_copy) {
            copyLog();
            return;
        }
        if (id == R.id.btn_patch_log_last) {
            showLastLog();
            return;
        }
        if (id == R.id.btn_patch_log_share) {
            shareLog();
            return;
        }
        if (id == R.id.tv_curpatch) {
            selectPatch();
            return;
        }
        if (id == R.id.btn_select_apply) {
            if (this.patchPath == null) {
                selectPatch();
                return;
            } else {
                applyPatch();
                return;
            }
        }
        if (id == R.id.tv_save_patches) {
            boolean ret = extractExamples("patch_app_rename.zip");
            if (ret | extractExamples("patch_data_editor.zip") | extractExamples("patch_new_entrance.zip") | extractExamples("patch_my_font.zip") | extractExamples("patch_mem_editor.zip") | extractExamples("patch_bypass_sigcheck.zip") | extractExamples("patch_launcher_toast.zip") | extractExamples("patch_script_example.zip")) {
                String message = String.format(this.activityRef.get().getString(R.string.patch_examples_copied), this.exampleDir);
                Toast.makeText((Context) this.activityRef.get(), message, 0).show();
            }
        }
    }

    /* JADX WARN: Type inference failed for: r0v2, types: [android.content.Context, com.gmail.heagoo.apkeditor.ApkInfoActivity] */
    private void previewPatch() {
        final ApkInfoActivity apkInfoActivity = this.activityRef.get();
        if (apkInfoActivity == null || this.patchRunning) {
            return;
        }
        if (this.patchPath == null) {
            Toast.makeText((Context) apkInfoActivity, (CharSequence) "Сначала выберите ZIP патча", 0).show();
            return;
        }
        final String path = this.patchPath;
        Toast.makeText((Context) apkInfoActivity, (CharSequence) "Проверяем описание патча…", 0).show();
        new Thread(new Runnable() {
            @Override
            public final void run() {
                PatchDialog.this.m253lambda$previewPatch$4$comgmailheagooapkeditorPatchDialog(apkInfoActivity, path);
            }
        }, "patch-preview").start();
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m253lambda$previewPatch$4$comgmailheagooapkeditorPatchDialog(final ApkInfoActivity apkInfoActivity, String path) {
        try {
            final PatchPreflight.Report report = PatchPreflight.inspectReport(apkInfoActivity, this, new File(path));
            apkInfoActivity.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m251lambda$previewPatch$2$comgmailheagooapkeditorPatchDialog(apkInfoActivity, report);
                }
            });
        } catch (Exception e) {
            final String message = "Не удалось прочитать описание патча. Проверьте, что выбран ZIP с файлом patch.txt.\n\n" + e;
            apkInfoActivity.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m252lambda$previewPatch$3$comgmailheagooapkeditorPatchDialog(apkInfoActivity, message);
                }
            });
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m251lambda$previewPatch$2$comgmailheagooapkeditorPatchDialog(final ApkInfoActivity apkInfoActivity, final PatchPreflight.Report report) {
        if (apkInfoActivity.isFinishing() || !isShowing()) {
            return;
        }
        new AlertDialog.Builder(apkInfoActivity).setTitle("Что делает патч").setMessage(report.summary).setNeutralButton("Подробности", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                PatchDialog.this.m250lambda$previewPatch$1$comgmailheagooapkeditorPatchDialog(apkInfoActivity, report, dialogInterface, i);
            }
        }).setPositiveButton("Понятно", (DialogInterface.OnClickListener) null).show();
    }

    void m250lambda$previewPatch$1$comgmailheagooapkeditorPatchDialog(ApkInfoActivity a, PatchPreflight.Report report, DialogInterface dialog, int which) {
        showPatchPreviewDetails(a, report.details);
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m252lambda$previewPatch$3$comgmailheagooapkeditorPatchDialog(ApkInfoActivity apkInfoActivity, String message) {
        if (apkInfoActivity.isFinishing() || !isShowing()) {
            return;
        }
        new AlertDialog.Builder(apkInfoActivity).setTitle("Не удалось проверить патч").setMessage(message).setPositiveButton("Понятно", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void showPatchPreviewDetails(ApkInfoActivity apkInfoActivity, String text) {
        TextView view = new TextView(apkInfoActivity);
        view.setText(text.isEmpty() ? "Описание не содержит действий." : text);
        view.setTextIsSelectable(true);
        int p = (int) (apkInfoActivity.getResources().getDisplayMetrics().density * 16.0f);
        view.setPadding(p, p, p, p);
        ScrollView scroll = new ScrollView(apkInfoActivity);
        scroll.addView(view);
        new AlertDialog.Builder(apkInfoActivity).setTitle("Действия и замечания").setView(scroll).setPositiveButton("Закрыть", (DialogInterface.OnClickListener) null).show();
    }

    private void initExampleDir() {
        if (this.exampleDir == null) {
            try {
                this.exampleDir = SDCard.makeDir((Context) this.activityRef.get(), "examples");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private boolean extractExamples(String filename) {
        initExampleDir();
        String path = this.exampleDir + filename;
        AssetManager am = this.activityRef.get().getAssets();
        InputStream input = null;
        FileOutputStream output = null;
        try {
            try {
                input = am.open(filename);
                output = new FileOutputStream(path);
                IOUtils.copy(input, output);
                closeQuietly(input);
                closeQuietly(output);
                return true;
            } catch (IOException e) {
                Toast.makeText((Context) this.activityRef.get(), e.getMessage(), 0).show();
                closeQuietly(input);
                closeQuietly(output);
                return false;
            }
        } catch (Throwable th) {
            closeQuietly(input);
            closeQuietly(output);
            throw th;
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    private void closeQuietly(ZipFile zfile) {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (IOException e) {
            }
        }
    }

    private void applyPatch() {
        this.errorCount.set(0);
        this.globalVariableValues.clear();
        try {
            this.logBuffer = PatchLogBuffer.start(getContext().getFilesDir());
            this.logSaveError = null;
            this.patchRunning = true;
            this.logTv.setText("");
            appendText(getContext().getString(R.string.patch_started_log, this.patchPath) + Document.LINE_SEPARATOR_UNIX, false, false);
            this.logLayout.setVisibility(0);
            this.webView.setVisibility(8);
            findViewById(R.id.btn_patch_log_last).setEnabled(false);
            findViewById(R.id.btn_patch_log_share).setEnabled(false);
            this.selectApplyBtn.setEnabled(false);
            this.selectApplyBtn.setText(R.string.patch_waiting);
            this.curPatchTv.setEnabled(false);
            this.patchExecutor = new PatchExecutor(this.activityRef.get(), this.patchPath, this);
            this.patchExecutor.applyPatch();
        } catch (IOException e) {
            Toast.makeText(getContext(), getContext().getString(R.string.patch_log_error, e.toString()), 1).show();
        }
    }

    private String shortNotice() {
        return getContext().getString(R.string.patch_log_excerpt_notice);
    }

    private void copyLog() {
        String text;
        boolean shortened;
        PatchLogBuffer current = this.logBuffer;
        if (current != null) {
            shortened = current.isTruncated();
            text = current.excerpt(shortNotice(), getContext().getString(R.string.patch_log_errors), getContext().getString(R.string.patch_log_end));
        } else {
            text = this.logTv.getText().toString();
            shortened = text.length() > 24000;
            if (shortened) {
                text = shortNotice() + "\n\n" + text.substring(text.length() - PatchLogBuffer.TAIL_LIMIT);
            }
        }
        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), R.string.patch_log_empty, 0).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService("clipboard");
        try {
        } catch (RuntimeException e) {
            try {
                String fallback = text.substring(Math.max(0, text.length() - 2048));
                clipboard.setPrimaryClip(ClipData.newPlainText(getContext().getString(R.string.patch_log_label), fallback));
                shortened = true;
            } catch (RuntimeException e2) {
                Toast.makeText(getContext(), R.string.patch_log_copy_failed, 1).show();
                return;
            }
        }
        if (clipboard == null) {
            throw new IllegalStateException("Clipboard unavailable");
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getContext().getString(R.string.patch_log_label), text));
        Toast.makeText(getContext(), shortened ? R.string.patch_log_copied_excerpt : R.string.patch_log_copied, 1).show();
    }

    private File mostRecentLog() {
        File last = new File(getContext().getFilesDir(), LAST_LOG_FILE);
        File pending = new File(getContext().getFilesDir(), "current_patch_log.txt");
        return (!pending.isFile() || (last.isFile() && pending.lastModified() <= last.lastModified())) ? last : pending;
    }

    private void showLastLog() {
        try {
            this.logBuffer = PatchLogBuffer.read(mostRecentLog());
            this.logLayout.setVisibility(0);
            this.webView.setVisibility(8);
            renderLog(this.logBuffer, false);
        } catch (FileNotFoundException e) {
            Toast.makeText(getContext(), R.string.patch_log_missing, 0).show();
        } catch (IOException e2) {
            Toast.makeText(getContext(), getContext().getString(R.string.patch_log_error, e2.toString()), 1).show();
        }
    }

    private void shareLog() {
        final ApkInfoActivity activity = this.activityRef.get();
        PatchLogBuffer selected = this.logBuffer;
        if (activity == null || this.patchRunning) {
            return;
        }
        final File source = selected != null ? selected.source() : mostRecentLog();
        if (source == null || !source.isFile()) {
            Toast.makeText(getContext(), R.string.patch_log_missing, 0).show();
        } else {
            findViewById(R.id.btn_patch_log_share).setEnabled(false);
            new Thread(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m258lambda$shareLog$7$comgmailheagooapkeditorPatchDialog(activity, source);
                }
            }, "share-patch-log").start();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m258lambda$shareLog$7$comgmailheagooapkeditorPatchDialog(final ApkInfoActivity apkInfoActivity, File source) {
        try {
            File dir = new File(apkInfoActivity.getCacheDir(), "patch-log-share");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Cannot create share directory");
            }
            File target = new File(dir, "patch-log.txt");
            InputStream in = new FileInputStream(source);
            try {
                FileOutputStream out = new FileOutputStream(target);
                try {
                    byte[] bytes = new byte[32768];
                    while (true) {
                        int n = in.read(bytes);
                        if (n == -1) {
                            out.close();
                            in.close();
                            Uri uri = FileProvider.getUriForFile(apkInfoActivity, apkInfoActivity.getPackageName() + ".fileprovider", target);
                            final Intent send = new Intent("android.intent.action.SEND").setType("text/plain").putExtra("android.intent.extra.STREAM", uri).addFlags(1);
                            int i = Build.VERSION.SDK_INT;
                            send.setClipData(ClipData.newRawUri("patch-log.txt", uri));
                            int i2 = Build.VERSION.SDK_INT;
                            apkInfoActivity.runOnUiThread(new Runnable() {
                                @Override
                                public final void run() {
                                    PatchDialog.this.m256lambda$shareLog$5$comgmailheagooapkeditorPatchDialog(apkInfoActivity, send);
                                }
                            });
                            return;
                        }
                        out.write(bytes, 0, n);
                    }
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            apkInfoActivity.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m257lambda$shareLog$6$comgmailheagooapkeditorPatchDialog(apkInfoActivity, e);
                }
            });
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m256lambda$shareLog$5$comgmailheagooapkeditorPatchDialog(ApkInfoActivity apkInfoActivity, Intent send) {
        findViewById(R.id.btn_patch_log_share).setEnabled(true);
        try {
            apkInfoActivity.startActivity(Intent.createChooser(send, apkInfoActivity.getString(R.string.patch_log_share_title)));
        } catch (RuntimeException e) {
            Toast.makeText((Context) apkInfoActivity, (CharSequence) apkInfoActivity.getString(R.string.patch_log_error, new Object[]{e.toString()}), 1).show();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m257lambda$shareLog$6$comgmailheagooapkeditorPatchDialog(ApkInfoActivity apkInfoActivity, Exception e) {
        findViewById(R.id.btn_patch_log_share).setEnabled(true);
        Toast.makeText((Context) apkInfoActivity, (CharSequence) apkInfoActivity.getString(R.string.patch_log_error, new Object[]{e.toString()}), 1).show();
    }

    private void renderLog(final PatchLogBuffer buffer, boolean autoScroll) {
        if (buffer == null || this.logBuffer != buffer) {
            return;
        }
        PatchLogBuffer.Preview preview = buffer.styledPreview(getContext().getString(R.string.patch_log_preview_notice));
        SpannableStringBuilder styled = new SpannableStringBuilder(preview.text);
        for (PatchLogBuffer.Range range : preview.ranges) {
            applyLogStyle(styled, range.start, range.end, range.style);
        }
        this.logTv.setText(styled);
        if (autoScroll) {
            this.logLayout.post(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m255lambda$renderLog$8$comgmailheagooapkeditorPatchDialog(buffer);
                }
            });
        }
    }

    void m255lambda$renderLog$8$comgmailheagooapkeditorPatchDialog(PatchLogBuffer buffer) {
        if (this.logBuffer == buffer && this.logLayout.getVisibility() == 0) {
            ((ScrollView) this.logLayout).fullScroll(130);
        }
    }

    private void queueLogUpdate(final PatchLogBuffer buffer) {
        if (this.logUpdatePending.compareAndSet(false, true)) {
            this.logHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    PatchDialog.this.m254lambda$queueLogUpdate$9$comgmailheagooapkeditorPatchDialog(buffer);
                }
            }, 120L);
        }
    }

    void m254lambda$queueLogUpdate$9$comgmailheagooapkeditorPatchDialog(PatchLogBuffer buffer) {
        this.logUpdatePending.set(false);
        renderLog(buffer, true);
    }

    private void selectPatch() {
        String defaultDir;
        initExampleDir();
        if (!new File(this.exampleDir).exists()) {
            defaultDir = null;
        } else {
            String defaultDir2 = this.exampleDir;
            defaultDir = defaultDir2;
        }
        FileSelectDialog dlg = new FileSelectDialog((Context) this.activityRef.get(), new FileSelectDialog.IFileSelection() {
            @Override
            public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                PatchDialog.this.patchSelected(filePath);
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return filename.endsWith(".zip");
            }

            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        }, ".zip", null, this.activityRef.get().getString(R.string.select_patch), false, false, false, "patch", defaultDir);
        dlg.show();
    }

    public void patchSelected(String filePath) {
        this.logBuffer = null;
        this.logLayout.setVisibility(0);
        this.webView.setVisibility(8);
        this.logTv.setText("");
        this.errorCount.set(0);
        this.globalVariableValues.clear();
        this.patchPathTv.setText(filePath);
        String patchConfig = getPatchConfig(filePath);
        if (patchConfig == null) {
            this.patchPath = null;
            this.selectApplyBtn.setText(R.string.select_patch);
        } else {
            this.logTv.setText(patchConfig);
            this.patchPath = filePath;
            this.selectApplyBtn.setText(R.string.apply_the_patch);
        }
        this.curPatchTv.setEnabled(true);
        this.selectApplyBtn.setEnabled(true);
    }

    private String getPatchConfig(String filePath) {
        ZipFile zfile = null;
        InputStream input = null;
        try {
            zfile = new ZipFile(filePath);
            ZipEntry entry = zfile.getEntry("patch.txt");
            if (entry == null) {
                error(R.string.patch_error_no_entry, "patch.txt");
                return null;
            }
            input = zfile.getInputStream(entry);
            return IOUtils.readString(input);
        } catch (Exception e) {
            error(R.string.general_error, e.getMessage());
            return null;
        } finally {
            closeQuietly(input);
            closeQuietly(zfile);
        }
    }

    public void info(int resourceId, boolean bold, Object... args) {
        StringBuilder sb;
        String txt = this.activityRef.get().getString(resourceId);
        if (args != null) {
            txt = String.format(txt, args);
        }
        if (bold) {
            sb = new StringBuilder();
            sb = sb.append(Document.LINE_SEPARATOR_UNIX);
        } else {
            sb = new StringBuilder();
        }
        String message = sb.append(txt).append(Document.LINE_SEPARATOR_UNIX).toString();
        int style = resourceId == R.string.all_rules_applied ? 3 : bold ? 1 : 0;
        appendStyledText(message, style);
    }

    public void info(String format, boolean bold, Object... args) {
        StringBuilder sb;
        String txt = format;
        if (args != null) {
            txt = String.format(txt, args);
        }
        if (bold) {
            sb = new StringBuilder();
            sb = sb.append(Document.LINE_SEPARATOR_UNIX);
        } else {
            sb = new StringBuilder();
        }
        String message = sb.append(txt).append(Document.LINE_SEPARATOR_UNIX).toString();
        appendText(message, bold, false);
    }

    public void error(int resourceId, Object... args) {
        this.errorCount.incrementAndGet();
        String txt = this.activityRef.get().getString(resourceId);
        if (args != null) {
            txt = String.format(txt, args);
        }
        appendText(txt + Document.LINE_SEPARATOR_UNIX, false, true);
    }

    private int logColor(int style) {
        TypedValue v = new TypedValue();
        if (style == 1 && getContext().getTheme().resolveAttribute(R.attr.labAccent, v, true)) {
            return v.data;
        }
        boolean light = getContext().getTheme().resolveAttribute(R.attr.labSurface, v, true) && ColorUtils.calculateLuminance(v.data) > 0.5d;
        if (style == 2) {
            return Color.parseColor(light ? "#B32638" : "#FF8793");
        }
        if (style == 3) {
            return Color.parseColor(light ? "#167044" : "#7CE1A7");
        }
        return Color.parseColor(light ? "#365CC5" : "#9BB8FF");
    }

    private void applyLogStyle(SpannableStringBuilder text, int start, int end, int style) {
        if (style == 0 || start < 0 || end > text.length() || start >= end) {
            return;
        }
        text.setSpan(new ForegroundColorSpan(logColor(style)), start, end, 33);
        text.setSpan(new StyleSpan(1), start, end, 33);
    }

    private void appendText(String txt, boolean bold, boolean red) {
        appendStyledText(txt, red ? 2 : bold ? 1 : 0);
    }

    private void appendStyledText(final String txt, final int style) {
        PatchLogBuffer buffer = this.logBuffer;
        if (buffer != null && this.patchRunning) {
            buffer.append(txt, style);
            queueLogUpdate(buffer);
        } else {
            ApkInfoActivity activity = this.activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public final void run() {
                        PatchDialog.this.m247xdaf7e3f5(txt, style);
                    }
                });
            }
        }
    }

    void m247xdaf7e3f5(String txt, int style) {
        SpannableStringBuilder styled = new SpannableStringBuilder(txt);
        applyLogStyle(styled, 0, txt.length(), style);
        this.logTv.append(styled);
    }

    public String getString(int stringId) {
        return this.activityRef.get().getString(stringId);
    }

    public void setVariableValue(String key, String value) {
        this.globalVariableValues.put(key, value);
    }

    public String getVariableValue(String key) {
        return this.globalVariableValues.get(key);
    }

    public boolean hasErrors() {
        return this.errorCount.get() > 0;
    }

    public void patchFinished() {
        final ApkInfoActivity activity = this.activityRef.get();
        if (activity == null) {
            return;
        }
        final boolean failed = hasErrors();
        if (failed) {
            appendText(Document.LINE_SEPARATOR_UNIX + activity.getString(R.string.patch_completed_with_errors) + Document.LINE_SEPARATOR_UNIX, true, true);
        }
        final PatchLogBuffer completed = this.logBuffer;
        if (completed != null) {
            try {
                completed.finish();
            } catch (IOException e) {
                this.logSaveError = e.toString();
            }
        }
        this.patchRunning = false;
        activity.runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                PatchDialog.this.m249lambda$patchFinished$11$comgmailheagooapkeditorPatchDialog(completed, failed, activity);
            }
        });
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m249lambda$patchFinished$11$comgmailheagooapkeditorPatchDialog(PatchLogBuffer completed, boolean failed, ApkInfoActivity apkInfoActivity) {
        renderLog(completed, true);
        this.curPatchTv.setEnabled(true);
        findViewById(R.id.btn_patch_log_last).setEnabled(true);
        findViewById(R.id.btn_patch_log_share).setEnabled(true);
        if (failed) {
            this.selectApplyBtn.setText(R.string.apply_the_patch);
            this.selectApplyBtn.setEnabled(true);
        } else {
            this.selectApplyBtn.setText(R.string.patch_applied);
            this.selectApplyBtn.setEnabled(false);
        }
        if (this.logSaveError != null) {
            Toast.makeText((Context) apkInfoActivity, (CharSequence) apkInfoActivity.getString(R.string.patch_log_error, new Object[]{this.logSaveError}), 1).show();
        }
    }

    public String getDecodeRootPath() {
        return this.activityRef.get().getDecodeRootPath();
    }

    public List<String> getSmaliFolders() {
        List<String> folders = new ArrayList<>();
        folders.add("smali");
        String apkPath = this.activityRef.get().getApkPath();
        ZipFile zfile = null;
        try {
            try {
                zfile = new ZipFile(apkPath);
                Enumeration<? extends ZipEntry> entries = zfile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".dex") && !name.contains("/") && !name.equals("classes.dex")) {
                        folders.add("smali_" + name.substring(0, name.length() - 4));
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return folders;
        } finally {
            closeQuietly(zfile);
        }
    }

    private void initManifestInfo() {
        String apkPath = this.activityRef.get().getApkPath();
        ZipFile zfile = null;
        InputStream input = null;
        try {
            try {
                zfile = new ZipFile(apkPath);
                ZipEntry zipEntry = zfile.getEntry("AndroidManifest.xml");
                input = zfile.getInputStream(zipEntry);
                ManifestParser parser = new ManifestParser(input);
                parser.parse();
                this.manifestInfo = parser.getManifestInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            closeQuietly(input);
            closeQuietly(zfile);
        }
    }

    public String getApplication() {
        String name;
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo == null || (name = this.manifestInfo.applicationCls) == null) {
            return null;
        }
        if (name.startsWith(".")) {
            return this.manifestInfo.packageName + name;
        }
        if (!name.contains(".")) {
            return this.manifestInfo.packageName + "." + name;
        }
        return name;
    }

    public List<String> getActivities() {
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo != null) {
            List<String> activities = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : this.manifestInfo.compNameIdx2Type.entrySet()) {
                if (entry.getValue().intValue() == 0) {
                    String name = getComponentName(this.manifestInfo, entry.getKey().intValue());
                    if (!activities.contains(name)) {
                        activities.add(name);
                    }
                }
            }
            return activities;
        }
        return null;
    }

    public List<String> getLauncherActivities() {
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo != null) {
            List<String> activities = new ArrayList<>();
            for (Integer idx : this.manifestInfo.launcherActIdxs) {
                String name = getComponentName(this.manifestInfo, idx.intValue());
                if (!activities.contains(name)) {
                    activities.add(name);
                }
            }
            return activities;
        }
        return null;
    }

    private String getComponentName(ManifestInfo manifestInfo, int index) {
        if (manifestInfo.targetActivityIdxs != null && manifestInfo.targetActivityIdxs.containsKey(Integer.valueOf(index))) {
            index = ((Integer) manifestInfo.targetActivityIdxs.get(Integer.valueOf(index))).intValue();
        }
        if (index < manifestInfo.strings.size()) {
            String name = (String) manifestInfo.strings.get(index);
            if (name.startsWith(".")) {
                return manifestInfo.packageName + name;
            }
            if (!name.contains(".")) {
                return manifestInfo.packageName + "." + name;
            }
            return name;
        }
        return null;
    }

    public List<String> getPatchNames() {
        if (this.patchExecutor != null) {
            return this.patchExecutor.getRuleNames();
        }
        return null;
    }
}
