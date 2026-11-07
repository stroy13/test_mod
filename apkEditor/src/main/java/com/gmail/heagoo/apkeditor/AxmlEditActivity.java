package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.ApkCreateActivity;
import com.gmail.heagoo.apkeditor.se.IDirChanged;
import com.gmail.heagoo.apkeditor.se.ZipFileListAdapter;
import com.gmail.heagoo.apkeditor.se.ZipHelper;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

public class AxmlEditActivity extends CustomizedLangActivity implements IDirChanged, View.OnClickListener {
    private ApkInfoParser.AppInfo apkInfo;
    private String apkPath;
    private ListView fileListView;
    private ZipFileListAdapter filesAdapter;
    private MyHandler handler;
    private Button saveBtn;
    private TextView summaryTv;
    private int themeId;
    private MyThread thread;
    private ZipHelper zipHelper;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        setContentView(R.layout.activity_axmledit);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");
        try {
            this.apkInfo = new ApkInfoParser().parse(this, this.apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            Toast.makeText((Context) this, (CharSequence) (msg + ": " + e.getMessage()), 1).show();
        }
        if (this.apkInfo != null) {
            this.handler = new MyHandler(this);
            this.thread = new MyThread(this);
            this.thread.start();
            initViews();
            return;
        }
        finish();
    }

    public void onDestroy() {
        if (this.filesAdapter != null) {
            this.filesAdapter.destroy();
        }
        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == 1) {
            String filePath = data.getStringExtra("xmlPath");
            String entryName = data.getStringExtra("extraString");
            new ProcessingDialog(this, new XmlCompiler(filePath, entryName), -1).show();
        }
    }

    public String getApkPath() {
        String packageName = getPackageName();
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            String apk = ai.publicSourceDir;
            return apk;
        } catch (Throwable th) {
            return null;
        }
    }

    public String getBinaryPath() {
        ApkComposeThread tmp = new ApkComposeThread(this, null, null, null);
        tmp.prepare();
        File fileDir = getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        return rootDirectory + "/bin/";
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString("apkPath", this.apkPath);
        super.onSaveInstanceState(outState);
    }

    public void dataReady(boolean succeed) {
        findViewById(R.id.progress_bar).setVisibility(8);
        if (succeed) {
            this.fileListView.setVisibility(0);
            initCenterView();
        } else {
            Toast.makeText((Context) this, (CharSequence) this.thread.getErrorMessage(), 0).show();
            finish();
        }
    }

    private void initCenterView() {
        this.filesAdapter = new ZipFileListAdapter(this, this, this.zipHelper, true);
        this.fileListView.setAdapter((ListAdapter) this.filesAdapter);
        this.fileListView.setOnItemClickListener(this.filesAdapter);
        this.fileListView.setOnItemLongClickListener(this.filesAdapter);
    }

    private void initViews() {
        this.fileListView = (ListView) findViewById(R.id.files_list);
        this.summaryTv = (TextView) findViewById(R.id.tv_summary);
        this.saveBtn = (Button) findViewById(R.id.btn_save);
        Button closeBtn = (Button) findViewById(R.id.btn_close);
        this.fileListView.setVisibility(4);
        this.saveBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        if (this.apkInfo != null) {
            ImageView apkIcon = (ImageView) findViewById(R.id.apk_icon);
            apkIcon.setImageDrawable(this.apkInfo.icon);
            TextView labelTV = (TextView) findViewById(R.id.apk_label);
            labelTV.setText(this.apkInfo.label);
        }
    }

    protected void viewProVersion() {
        String pkgName = getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent("android.intent.action.VIEW", uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + pkgName)));
        }
    }

    public void initData() throws Exception {
        this.zipHelper = new ZipHelper(this.apkPath);
        this.zipHelper.parse();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
        } else if (id == R.id.btn_save) {
            makeAPK();
            finish();
        }
    }

    private void makeAPK() {
        Map<String, String> fileReplaces = this.filesAdapter.getReplaces();
        Intent intent = new Intent((Context) this, (Class<?>) ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtil.attachParam(intent, "packageName", this.apkInfo.packageName);
        if (!fileReplaces.isEmpty()) {
            ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
        }
        startActivity(intent);
    }

    public void dirChanged(String dir) {
        this.summaryTv.setText(dir);
    }

    private static class MyHandler extends Handler {
        WeakReference<AxmlEditActivity> activityRef;

        public MyHandler(AxmlEditActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AxmlEditActivity activity = this.activityRef.get();
            if (activity == null) {
            }
            switch (msg.what) {
                case 0:
                    activity.dataReady(true);
                    break;
                case 1:
                    activity.dataReady(false);
                    break;
            }
        }
    }

    private static class MyThread extends Thread {
        WeakReference<AxmlEditActivity> activityRef;
        String err;

        public MyThread(AxmlEditActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            AxmlEditActivity activity = this.activityRef.get();
            if (activity != null) {
                try {
                    activity.initData();
                    activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.err = e.getMessage();
                    activity.handler.sendEmptyMessage(1);
                }
            }
        }

        String getErrorMessage() {
            return this.err;
        }
    }

    private class XmlCompiler implements ProcessingDialog.ProcessingInterface {
        private String axmlPath;
        private String entryName;
        private String errMessage;
        private String outMessage;
        private boolean succeed = false;
        private String tempPath;
        private String xmlPath;

        public XmlCompiler(String filePath, String entryName) {
            this.xmlPath = filePath;
            this.axmlPath = filePath + ".bin";
            this.entryName = entryName;
            this.tempPath = filePath + RandomUtil.getRandomString(6);
        }

        @Override
        public void process() throws Exception {
            String binaryPath = AxmlEditActivity.this.getBinaryPath();
            String aaptPath = ApkComposeThread.getNativeAaptPath(AxmlEditActivity.this);
            String androidPath = binaryPath + "android.jar";
            CommandRunner cr = new CommandRunner();
            cr.runCommand(new String[]{aaptPath, "z", "-I", androidPath, this.xmlPath, this.tempPath, AxmlEditActivity.this.getApkPath()}, (String[]) null, (String) null, 5000, false);
            this.outMessage = cr.getStdOut();
            this.errMessage = cr.getStdError();
            File generated = new File(this.tempPath);
            if (generated.exists()) {
                this.succeed = true;
                File targetFile = new File(this.axmlPath);
                boolean ret = generated.renameTo(targetFile);
                if (!ret) {
                    generated.delete();
                }
            }
        }

        @Override
        public void afterProcess() {
            if (this.succeed && new File(this.axmlPath).exists()) {
                String str = String.format(AxmlEditActivity.this.getString(R.string.entry_modified), this.entryName);
                Toast.makeText((Context) AxmlEditActivity.this, (CharSequence) str, 0).show();
                AxmlEditActivity.this.filesAdapter.addReplace(this.entryName, this.axmlPath);
                AxmlEditActivity.this.saveBtn.setVisibility(0);
                return;
            }
            String message = this.outMessage;
            if (this.errMessage != null && !this.errMessage.equals("")) {
                message = this.errMessage;
            }
            new AlertDialog.Builder(AxmlEditActivity.this).setTitle(R.string.error).setMessage(message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }
}
