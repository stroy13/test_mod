package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.util.OdexPatcher;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.EditorTheme;
import java.io.File;
import java.util.Locale;

public class OdexPatchActivity extends Activity implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private EditText apkPathEt;

    @Override
    public void onCreate(Bundle bundle) {
        EditorTheme.apply(this);
        super.onCreate(bundle);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(R.layout.activity_odex_patch);
        this.apkPathEt = (EditText) findViewById(R.id.et_apkpath);
        findViewById(R.id.odex_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                OdexPatchActivity.this.m245lambda$onCreate$0$comgmailheagooapkeditorOdexPatchActivity(view);
            }
        });
        findViewById(R.id.btn_select_apkpath).setOnClickListener(this);
        findViewById(R.id.btn_apply_patch).setOnClickListener(this);
    }

    void m245lambda$onCreate$0$comgmailheagooapkeditorOdexPatchActivity(View v) {
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_select_apkpath) {
            new FileSelectDialog(this, this, ".apk", "", null).show();
            return;
        }
        if (v.getId() == R.id.btn_apply_patch) {
            final String path = this.apkPathEt.getText().toString().trim();
            File file = new File(path);
            if (path.isEmpty() || !path.toLowerCase(Locale.ROOT).endsWith(".apk") || !file.isFile() || !file.canRead()) {
                Toast.makeText(this, R.string.odex_invalid_apk, 1).show();
            } else {
                new AlertDialog.Builder(this).setTitle(R.string.odex_patcher_title).setMessage(R.string.odex_confirm).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.apply_the_patch, new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        OdexPatchActivity.this.m244lambda$onClick$1$comgmailheagooapkeditorOdexPatchActivity(path, dialogInterface, i);
                    }
                }).show();
            }
        }
    }

    void m244lambda$onClick$1$comgmailheagooapkeditorOdexPatchActivity(String path, DialogInterface dialog, int which) {
        new ProcessingDialog(this, new PatchProcessor(path), -1).show();
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        this.apkPathEt.setText(filePath);
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".apk");
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    private final class PatchProcessor implements ProcessingDialog.ProcessingInterface {
        private final String apkPath;
        private String odexPath;
        private boolean succeeded;

        PatchProcessor(String apkPath) {
            this.apkPath = apkPath;
        }

        @Override
        public void process() throws Exception {
            try {
                ApkInfoParser.AppInfo info = new ApkInfoParser().parse(OdexPatchActivity.this, this.apkPath);
                if (info == null || info.packageName == null || info.packageName.isEmpty()) {
                    throw new Exception(OdexPatchActivity.this.getString(R.string.odex_parse_failed));
                }
                OdexPatcher patcher = new OdexPatcher(info.packageName);
                patcher.applyPatch(OdexPatchActivity.this, this.apkPath);
                if (patcher.errMessage != null) {
                    throw new Exception(patcher.errMessage);
                }
                if (patcher.targetOdex == null) {
                    throw new Exception(OdexPatchActivity.this.getString(R.string.odex_failed));
                }
                this.odexPath = patcher.targetOdex;
                this.succeeded = true;
            } catch (Exception e) {
                String message = e.getMessage();
                throw new Exception((message == null || message.isEmpty()) ? OdexPatchActivity.this.getString(R.string.odex_failed) : message, e);
            }
        }

        @Override
        public void afterProcess() {
            if (this.succeeded) {
                Toast.makeText(OdexPatchActivity.this, OdexPatchActivity.this.getString(R.string.odex_success, new Object[]{this.odexPath}), 1).show();
            }
        }
    }
}
