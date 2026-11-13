package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ApkComposeService;
import com.gmail.heagoo.apkeditor.Constants;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.se.ApkInstaller;
import com.gmail.heagoo.apkeditor.util.AxmlStringModifier;
import com.gmail.heagoo.apkeditor.util.ErrorFixManager;
import com.gmail.heagoo.apkeditor.util.OdexPatcher;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.PackageUtil;
import com.gmail.heagoo.common.PreferenceUtil;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

public class ApkComposeActivity extends CustomizedLangActivity implements ITaskCallback, View.OnClickListener {
    public static final int FAILED = -1;
    public static final int PRIMARY_FOREGROUND_NOTIF_SERVICE_ID = 1001;
    public static final String PRIMARY_NOTIF_CHANNEL = "default";
    public static final int SUCCEED = 10005;
    private Button bgButton;
    private android.widget.ListView buildLogList;
    private android.widget.ArrayAdapter<String> buildLogAdapter;
    private Button buildLogToggle;
    private boolean logExpanded = true;
    private File buildLogFile;
    private long buildLogStart;
    private int logGeneration;
    private com.gmail.heagoo.apkeditor.util.BuildLogTail logTail = new com.gmail.heagoo.apkeditor.util.BuildLogTail();
    private final java.util.concurrent.ExecutorService logReader = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler logHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean logReadInFlight;
    private final Runnable pollLog = this::pollBuildLog;
    private ApkComposeService.ComposeServiceBinder binder;
    private boolean codeModified;
    private LinearLayout composedLayout;
    private LinearLayout composingLayout;
    private Button copyBtn;
    private String decodeRootPath;
    private ErrorFixManager errFixer;
    private String errMessage;
    private ListView failedLv;
    private Button fixBtn;
    private LinearLayout fixLayout;
    private TextView fixTipTv;
    private Button hideWarningBtn;
    private LinearLayout buildWarningLayout;
    private TextView buildWarningTv;
    private final ArrayList<String> buildWarnings = new ArrayList<>();
    private String intentAction;
    private boolean isActivityVisible;
    private MyHandler msgHandler;
    private String packageName;
    private Button patchBtn;
    private TextView patchTip;
    private TextView progressTv;
    private Button removeBtn;
    private ImageView resultImgView;
    private TextView resultTv;
    private boolean signAPK;
    protected String srcApkPath;
    private long startTime;
    private ITaskCallback.TaskStepInfo stepInfo;
    private String targetApkPath;
    public boolean createdFromNotification = false;
    private boolean patchSucceed = false;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApkComposeActivity.this.binder = (ApkComposeService.ComposeServiceBinder) service;
            if (ApkComposeActivity.this.intentAction != null && Constants.ACTION.MAIN_ACTION.equals(ApkComposeActivity.this.intentAction)) {
                ApkComposeActivity.this.createdFromNotification = true;
                if (!ApkComposeActivity.this.binder.isRunning()) {
                    ApkComposeActivity.this.binder.hideNotification();
                }
                ApkComposeActivity.this.bgButton.setVisibility(0);
            }
            ApkComposeActivity.this.binder.setObserver(ApkComposeActivity.this);
            Map<String, Object> keyValues = ApkComposeActivity.this.binder.getValues();
            ApkComposeActivity.this.srcApkPath = (String) keyValues.get("srcApkPath");
            ApkComposeActivity.this.targetApkPath = (String) keyValues.get("targetApkPath");
            Long logStart = (Long) keyValues.get("logBuildStartedAt");
            ApkComposeActivity.this.startBuildLog(logStart == null ? 0 : logStart);
            ApkComposeActivity.this.decodeRootPath = (String) keyValues.get("decodeRootPath");
            ApkComposeActivity.this.codeModified = ((Boolean) keyValues.get("codeModified")).booleanValue();
            ApkComposeActivity.this.signAPK = ((Boolean) keyValues.get("signAPK")).booleanValue();
            ApkComposeActivity.this.errFixer = new ErrorFixManager(ApkComposeActivity.this.decodeRootPath);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ApkComposeActivity.this.binder = null;
            ApkComposeActivity.this.taskFailed(ApkComposeActivity.this.getString(R.string.lab_compose_service_lost));
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationAccess.createBuildChannel(this);
        Intent intent = getIntent();
        this.intentAction = intent.getAction();
        getWindow().setFlags(BinaryDiffIndex.PAGE_ROWS, BinaryDiffIndex.PAGE_ROWS);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setContentView(R.layout.activity_apkcompose);
        initView();
        if (savedInstanceState != null) logExpanded = savedInstanceState.getBoolean("build_log_expanded", true);
        updateLogVisibility();
        bind2Service();
        this.startTime = System.currentTimeMillis();
    }

    public void onNewIntent(Intent intent) {
        this.intentAction = intent.getAction();
        if (this.intentAction != null && Constants.ACTION.MAIN_ACTION.equals(this.intentAction) && this.binder != null && !this.binder.isRunning()) {
            this.binder.hideNotification();
        }
    }

    private void bind2Service() {
        Intent intent = new Intent((Context) this, (Class<?>) ApkComposeService.class);
        try {
            if (bindService(intent, this.connection, 1)) {
                return;
            }
        } catch (RuntimeException error) {
            Log.e("ApkComposeActivity", "Cannot connect to build service", error);
        }
        this.connection = null;
        taskFailed(getString(R.string.lab_compose_service_lost));
    }

    public void onPause() {
        this.isActivityVisible = false;
        logHandler.removeCallbacks(pollLog);
        super.onPause();
    }

    protected void onDestroy() {
        logGeneration++;
        logHandler.removeCallbacksAndMessages(null);
        logReader.shutdownNow();
        if (this.connection != null) {
            unbindService(this.connection);
            this.connection = null;
        }
        super.onDestroy();
    }

    protected void onResume() {
        super.onResume();
        this.isActivityVisible = true;
        scheduleLogRead();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("build_log_expanded", logExpanded);
        super.onSaveInstanceState(out);
    }

    private void initView() {
        this.msgHandler = new MyHandler(this);
        buildLogList = findViewById(R.id.build_log_list);
        buildLogToggle = findViewById(R.id.build_log_toggle);
        buildLogAdapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextSize(11); text.setTypeface(android.graphics.Typeface.MONOSPACE);
                text.setTextColor(progressLogColor());
                text.setPadding(12, 2, 12, 2);
                return text;
            }
        };
        buildLogList.setAdapter(buildLogAdapter);
        buildLogToggle.setOnClickListener(v -> { logExpanded = !logExpanded; updateLogVisibility(); });
        this.composingLayout = (LinearLayout) findViewById(R.id.layout_apk_composing);
        this.composedLayout = (LinearLayout) findViewById(R.id.layout_apk_composed);
        this.progressTv = (TextView) findViewById(R.id.progress_tip);
        this.resultTv = (TextView) findViewById(R.id.result);
        this.failedLv = (ListView) findViewById(R.id.failed_view);
        this.resultImgView = (ImageView) findViewById(R.id.result_image);
        this.fixLayout = (LinearLayout) findViewById(R.id.fix_layout);
        this.fixTipTv = (TextView) findViewById(R.id.tv_fix_tip);
        this.resultTv.setOnClickListener(this);
        this.composedLayout.setVisibility(4);
        Button closeBtn = (Button) findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        this.removeBtn = (Button) findViewById(R.id.btn_remove);
        this.removeBtn.setOnClickListener(this);
        this.fixBtn = (Button) findViewById(R.id.btn_fix);
        this.fixBtn.setOnClickListener(this);
        this.copyBtn = (Button) findViewById(R.id.btn_copy_errmsg);
        this.copyBtn.setOnClickListener(this);
        this.hideWarningBtn = (Button) findViewById(R.id.btn_hide_warning);
        this.hideWarningBtn.setOnClickListener(this);
        this.bgButton = (Button) findViewById(R.id.btn_bg);
        this.bgButton.setOnClickListener(this);
        this.buildWarningLayout = (LinearLayout) findViewById(R.id.layout_build_warning);
        this.buildWarningTv = (TextView) findViewById(R.id.tv_build_warning);
        findViewById(R.id.btn_copy_build_warning).setOnClickListener(this);
        findViewById(R.id.btn_dismiss_build_warning).setOnClickListener(this);
    }

    private int progressLogColor() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.labText, value, true);
        return value.data;
    }

    private void updateLogVisibility() {
        buildLogList.setVisibility(logExpanded ? View.VISIBLE : View.GONE);
        View panel = findViewById(R.id.build_log_panel);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) panel.getLayoutParams();
        params.height = logExpanded ? 0 : android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        params.weight = logExpanded ? 1f : 0f;
        panel.setLayoutParams(params);
        buildLogToggle.setText(logExpanded ? R.string.build_log_collapse : R.string.build_log_expand);
    }

    private void startBuildLog(long started) {
        logGeneration++;
        logHandler.removeCallbacks(pollLog);
        logTail = new com.gmail.heagoo.apkeditor.util.BuildLogTail();
        buildLogStart = started;
        buildLogFile = targetApkPath == null ? null : new File(new File(targetApkPath).getAbsoluteFile().getParentFile(), "build-log.txt");
        buildLogAdapter.clear();
        buildLogAdapter.notifyDataSetChanged();
        scheduleLogRead();
    }

    private void scheduleLogRead() {
        if (isActivityVisible && buildLogFile != null && !logReadInFlight) {
            logHandler.removeCallbacks(pollLog);
            logHandler.postDelayed(pollLog, 500);
        }
    }

    private void pollBuildLog() {
        if (!isActivityVisible || buildLogFile == null || logReadInFlight) return;
        logReadInFlight = true;
        final int generation = logGeneration;
        final File file = buildLogFile;
        final long started = buildLogStart;
        final com.gmail.heagoo.apkeditor.util.BuildLogTail reader = logTail;
        logReader.execute(() -> {
            java.util.List<String> lines = java.util.Collections.emptyList();
            try {
                if (file.isFile() && file.lastModified() >= started) lines = reader.read(file);
            } catch (java.io.IOException ignored) { }
            final java.util.List<String> batch = lines;
            logHandler.post(() -> {
                logReadInFlight = false;
                if (generation == logGeneration && !isFinishing()) {
                    boolean atBottom = buildLogAdapter.isEmpty() || buildLogList.getLastVisiblePosition() >= buildLogAdapter.getCount() - 2;
                    buildLogAdapter.setNotifyOnChange(false);
                    for (String line : batch) {
                        if ("\u0000RESET".equals(line)) buildLogAdapter.clear();
                        else {
                            String preference = android.preference.PreferenceManager.getDefaultSharedPreferences(this).getString("BuildLogLanguage", "system");
                            String systemLanguage = android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
                            String lang = com.gmail.heagoo.apkeditor.util.BuildLogDisplay.resolveLanguage(preference, systemLanguage);
                            buildLogAdapter.add(com.gmail.heagoo.apkeditor.util.BuildLogDisplay.render(line, lang));
                        }
                    }
                    if (!batch.isEmpty()) buildLogAdapter.notifyDataSetChanged();
                    if (!batch.isEmpty() && atBottom && logExpanded) buildLogList.setSelection(buildLogAdapter.getCount() - 1);
                }
                scheduleLogRead();
            });
        });
    }

    public void setTaskStepInfo(ITaskCallback.TaskStepInfo stepInfo) {
        this.stepInfo = stepInfo;
        this.msgHandler.sendEmptyMessage(1);
    }

    public void setTaskProgress(float progress) {
    }

    public void taskSucceed() {
        this.msgHandler.sendEmptyMessage(2);
    }

    public void taskFailed(String errMessage) {
        this.errMessage = errMessage;
        this.msgHandler.sendEmptyMessage(3);
    }

    public void taskWarning(String message) {
        this.msgHandler.warning(message);
    }

    private void showBuildWarning(String message) {
        if (message == null || message.trim().isEmpty() || this.buildWarnings.contains(message)) {
            return;
        }
        this.buildWarnings.add(message);
        StringBuilder sb = new StringBuilder();
        for (String warning : this.buildWarnings) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(warning);
        }
        this.buildWarningTv.setText(sb.toString());
        this.buildWarningLayout.setVisibility(View.VISIBLE);
    }

    private void dismissBuildWarnings() {
        this.buildWarnings.clear();
        this.buildWarningTv.setText("");
        this.buildWarningLayout.setVisibility(View.GONE);
        if (this.binder != null) {
            this.binder.clearWarnings();
        }
    }

    private String buildWarningText() {
        StringBuilder sb = new StringBuilder();
        for (String warning : this.buildWarnings) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(warning);
        }
        return sb.toString();
    }

    public void updateComposeInfo() {
        this.progressTv.setText(String.format(getResources().getString(R.string.step) + " %d/%d: %s", Integer.valueOf(this.stepInfo.stepIndex), Integer.valueOf(this.stepInfo.stepTotal), this.stepInfo.stepDescription));
    }

    public boolean isFreeVersion() {
        return false;
    }

    public void composeFinished(boolean ret) {
        this.composingLayout.setVisibility(4);
        this.composedLayout.setVisibility(0);
        if (this.binder != null && this.isActivityVisible) {
            this.binder.hideNotification();
        }
        Button installBtn = (Button) findViewById(R.id.btn_install);
        if (ret) {
            setResult(SUCCEED);
            installBtn.setOnClickListener(this);
            findViewById(R.id.succeeded_view).setVisibility(0);
            findViewById(R.id.failed_view).setVisibility(8);
            this.resultImgView.setImageResource(R.drawable.succeed);
            String str = getResources().getString(R.string.apk_savedas_1);
            String strPlace = String.format(str, this.targetApkPath);
            String strSucceed = getResources().getString(R.string.succeed);
            String message = strSucceed + "!\n" + strPlace + "\n\n";
            if (this.signAPK) {
                this.packageName = getApkPackageName();
                if (isPackageInstalled(this.packageName)) {
                    String allStr = message + getResources().getString(R.string.remove_tip);
                    SpannableStringBuilder style = new SpannableStringBuilder(allStr);
                    style.setSpan(new AbsoluteSizeSpan(22), message.length(), allStr.length(), 33);
                    this.resultTv.setText(style);
                    this.removeBtn.setVisibility(0);
                } else {
                    this.resultTv.setText(message);
                    this.removeBtn.setVisibility(8);
                }
                this.copyBtn.setVisibility(8);
                installBtn.setVisibility(0);
            } else {
                String allStr2 = message + getResources().getString(R.string.not_signed_tip);
                SpannableStringBuilder style2 = new SpannableStringBuilder(allStr2);
                style2.setSpan(new AbsoluteSizeSpan(22), message.length(), allStr2.length(), 18);
                ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(ParserRule.MAJOR_ACTIONS, 30, 30));
                style2.setSpan(fcs, message.length(), allStr2.length(), 18);
                this.resultTv.setText(style2);
                this.copyBtn.setVisibility(8);
                this.removeBtn.setVisibility(8);
                installBtn.setVisibility(8);
            }
            if (!this.codeModified || !isArtRuntime()) {
                findViewById(R.id.patch_dex_layout).setVisibility(8);
                return;
            }
            this.patchTip = (TextView) findViewById(R.id.tv_patch_tip);
            findViewById(R.id.patch_dex_layout).setVisibility(0);
            this.patchBtn = (Button) findViewById(R.id.btn_patch);
            this.patchBtn.setOnClickListener(this);
            return;
        }
        setResult(-1);
        findViewById(R.id.succeeded_view).setVisibility(8);
        findViewById(R.id.failed_view).setVisibility(0);
        this.failedLv.setDivider(null);
        this.failedLv.setAdapter((ListAdapter) new ApkComposeFailAdapter(this, this.errMessage));
        Log.d("sawsem", this.errMessage);
        this.resultImgView.setImageResource(R.drawable.failed);
        if (!this.errMessage.contains("warning:")) {
            this.hideWarningBtn.setVisibility(8);
        } else {
            this.hideWarningBtn.setVisibility(0);
        }
        if (this.errFixer != null) {
            this.errFixer.setErrMessage(this.errMessage);
        }
        if (this.errFixer != null && this.errFixer.isErrorFixable()) {
            int resId = getTipResourceId();
            this.fixTipTv.setText(resId);
            this.fixLayout.setVisibility(0);
        }
        installBtn.setVisibility(8);
        this.removeBtn.setVisibility(8);
        this.copyBtn.setVisibility(0);
    }

    private boolean isArtRuntime() {
        String vmVersion = System.getProperty("java.vm.version");
        return vmVersion != null && vmVersion.charAt(0) > '1';
    }

    private int getTipResourceId() {
        int fid = this.errFixer.getFixerId();
        switch (fid) {
            case 0:
                int resId = R.string.fix_invalid_name_tip;
                return resId;
            case 1:
                int resId2 = R.string.fix_invalid_token_tip;
                return resId2;
            case 2:
                int resId3 = R.string.fix_invalid_attr_tip;
                return resId3;
            case 3:
                int resId4 = R.string.fix_invalid_symbol_tip;
                return resId4;
            case 4:
                int resId5 = R.string.fix_error_equivalent;
                return resId5;
            default:
                return -1;
        }
    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager pkgMgr = getPackageManager();
        try {
            pkgMgr.getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getApkPackageName() {
        try {
            ApkInfoParser.AppInfo info = new ApkInfoParser().parse(this, this.targetApkPath);
            if (info != null) {
                return info.packageName;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
            return;
        }
        if (id == R.id.btn_install) {
            ApkInstaller.install(this, this.targetApkPath);
            return;
        }
        if (id == R.id.btn_remove) {
            if (this.packageName != null) {
                PackageUtil.uninstallPackage(this, this.packageName);
                return;
            }
            return;
        }
        if (id == R.id.btn_copy_errmsg) {
            ClipboardUtil.copyToClipboard(this, this.errMessage);
            Toast.makeText((Context) this, R.string.errmsg_copied, 0).show();
            return;
        }
        if (id == R.id.btn_fix) {
            if (this.errFixer != null) {
                this.fixLayout.setVisibility(8);
                this.errFixer.fixErrors(this);
                return;
            }
            return;
        }
        if (id == R.id.btn_patch) {
            if (!this.patchSucceed) {
                applyCodePatch();
                return;
            } else {
                launchApp();
                return;
            }
        }
        if (id == R.id.btn_hide_warning) {
            StringBuilder sb = new StringBuilder();
            String[] lines = this.errMessage.split(Document.LINE_SEPARATOR_UNIX);
            for (String line : lines) {
                if (!line.startsWith("warning:")) {
                    sb.append(line);
                    sb.append(Document.LINE_SEPARATOR_UNIX);
                }
            }
            ApkComposeFailAdapter adapter = (ApkComposeFailAdapter) this.failedLv.getAdapter();
            adapter.updateMessage(sb.toString());
            adapter.notifyDataSetChanged();
            this.hideWarningBtn.setVisibility(8);
            return;
        }
        if (id == R.id.btn_copy_build_warning) {
            ClipboardUtil.copyToClipboard(this, buildWarningText());
            Toast.makeText((Context) this, R.string.build_warning_copied, Toast.LENGTH_SHORT).show();
            return;
        }
        if (id == R.id.btn_dismiss_build_warning) {
            dismissBuildWarnings();
            return;
        }
        if (id == R.id.btn_bg) {
            finish();
        } else if (id == R.id.result && this.targetApkPath != null) {
            showFileInExplorer(this.targetApkPath);
        }
    }

    private void showFileInExplorer(String filepath) {
        int position;
        if (!new File(filepath).exists() || (position = filepath.lastIndexOf("/")) == -1) {
            return;
        }
        String path = filepath.substring(0, position + 1);
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.addFlags(268435456);
        intent.setDataAndType(Uri.fromFile(file), "text/csv");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void launchApp() {
        PackageManager pm = getPackageManager();
        try {
            Intent it = pm.getLaunchIntentForPackage(this.packageName);
            if (it != null) {
                startActivity(it);
            }
        } catch (ActivityNotFoundException e) {
        }
    }

    private void applyCodePatch() {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            private String errMessage = null;
            private String targetOdex = null;

            @Override
            public void process() throws Exception {
                OdexPatcher patcher = new OdexPatcher(ApkComposeActivity.this.packageName);
                patcher.applyPatch(ApkComposeActivity.this, ApkComposeActivity.this.targetApkPath);
                this.targetOdex = patcher.targetOdex;
                if (patcher.errMessage != null) {
                    this.errMessage = patcher.errMessage;
                    throw new Exception(this.errMessage);
                }
            }

            @Override
            public void afterProcess() {
                if (this.errMessage == null) {
                    ApkComposeActivity.this.patchSucceed = true;
                    String fmt = ApkComposeActivity.this.getString(R.string.patch_code_cache_done);
                    String msg = String.format(fmt, this.targetOdex);
                    ApkComposeActivity.this.patchTip.setText(msg);
                    ApkComposeActivity.this.patchBtn.setText(R.string.launch);
                    return;
                }
                ApkComposeActivity.this.patchTip.setText(this.errMessage);
            }
        }, -1).show();
    }

    public void buildAgain() {
        if (this.binder != null) {
            Map<String, Map<String, String>> m = this.errFixer.getModifications();
            if (this.binder != null && !m.isEmpty()) {
                this.binder.setBuildHooker(createBuildHooker(m));
            }
            this.progressTv.setText("");
            dismissBuildWarnings();
            this.composingLayout.setVisibility(0);
            this.composedLayout.setVisibility(8);
            this.binder.buildAgain();
            Map<String, Object> values = this.binder.getValues();
            Long started = (Long) values.get("logBuildStartedAt");
            startBuildLog(started == null ? 0 : started);
        }
    }

    private IApkMaking createBuildHooker(Map<String, Map<String, String>> modifications) {
        return new AxmlStringModifier(this.decodeRootPath, modifications);
    }

    private void showTipDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dlg_tip, (ViewGroup) null);
        TextView msgTv = (TextView) view.findViewById(R.id.tv_message);
        msgTv.setText(R.string.build_still_running_tip);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.cb_show_once);
        AlertDialog.Builder tipDlg = new AlertDialog.Builder(this).setTitle(R.string.tip).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (ApkComposeActivity.this.binder != null && ApkComposeActivity.this.binder.isRunning()) {
                    ApkComposeActivity.this.finish();
                }
                if (cb.isChecked()) {
                    PreferenceUtil.setBoolean(ApkComposeActivity.this, "donot_show_compose_tip", true);
                }
            }
        });
        tipDlg.setView(view);
        tipDlg.show();
    }

    public void stopBuildAndGoBack() {
        try {
            if (this.binder != null) {
                this.binder.stopBuilding();
            }
            unbindService(this.connection);
            this.connection = null;
            Intent intent = new Intent((Context) this, (Class<?>) ApkComposeService.class);
            stopService(intent);
        } catch (Throwable th) {
        }
        finish();
    }

    private void showStopBuildDialog() {
        AlertDialog.Builder confirmDlg = new AlertDialog.Builder(this).setTitle(R.string.please_note).setMessage(R.string.sure_to_stop_build).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ApkComposeActivity.this.stopBuildAndGoBack();
            }
        }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null);
        confirmDlg.show();
    }

    public void onBackPressed() {
        if (this.binder != null && this.binder.isRunning()) {
            if (!PreferenceUtil.getBoolean(this, "donot_show_compose_tip", false)) {
                showTipDialog();
                return;
            } else {
                finish();
                return;
            }
        }
        finish();
    }

    private static class MyHandler extends Handler {
        public static final int FAILED = 3;
        public static final int SHOW_MESSAGE = 4;
        public static final int SHOW_WARNING = 5;
        public static final int STEPINFO = 1;
        public static final int SUCCEED = 2;
        private WeakReference<ApkComposeActivity> activityRef;
        private String tmpMessage;

        public MyHandler(ApkComposeActivity a) {
            this.activityRef = new WeakReference<>(a);
        }

        public void warning(String msg) {
            sendMessage(obtainMessage(SHOW_WARNING, msg));
        }

        public void toast(String msg) {
            if ("x".equals(msg)) {
                this.tmpMessage = getString();
            } else {
                this.tmpMessage = msg;
            }
            sendEmptyMessage(4);
        }

        private String getString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Nbojgftu!fejujoh!jt!ejtbcmfe!)tffnt!opu!b!hfovjof!wfstjpo*");
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                sb.setCharAt(i, (char) (c - 1));
            }
            return sb.toString();
        }

        @Override
        public void handleMessage(Message msg) {
            ApkComposeActivity apkComposeActivity = this.activityRef.get();
            if (apkComposeActivity != null) {
                switch (msg.what) {
                    case 1:
                        apkComposeActivity.updateComposeInfo();
                        break;
                    case 2:
                        apkComposeActivity.composeFinished(true);
                        break;
                    case 3:
                        apkComposeActivity.composeFinished(false);
                        break;
                    case 4:
                        Toast.makeText((Context) apkComposeActivity, (CharSequence) this.tmpMessage, 1).show();
                        break;
                    case SHOW_WARNING:
                        apkComposeActivity.showBuildWarning((String) msg.obj);
                        break;
                }
            }
        }
    }
}
