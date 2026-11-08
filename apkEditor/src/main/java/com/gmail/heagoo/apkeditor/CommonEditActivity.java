package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.ApkParser;
import com.gmail.heagoo.apkeditor.ce.DexDecode;
import com.gmail.heagoo.apkeditor.ce.DexEncode;
import com.gmail.heagoo.apkeditor.ce.DexPatch_CheatPkgName;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.ManifestInfo;
import com.gmail.heagoo.apkeditor.ce.ManifestParser;
import com.gmail.heagoo.apkeditor.ce.ReplaceLauncherIcon;
import com.gmail.heagoo.apkeditor.ce.e.ManifestEditorNew;
import com.gmail.heagoo.apkeditor.ce.e.PluginWrapperExtra;
import com.gmail.heagoo.apkeditor.ce.e.PluginWrapperMfEditor;
import com.gmail.heagoo.apkeditor.ce.e.RefactorDex;
import com.gmail.heagoo.apkeditor.ce.e.RefactorLayout;
import com.gmail.heagoo.apkeditor.se.ApkCreateActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.FileEncrypter;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonEditActivity extends CustomizedLangActivity implements View.OnClickListener, FileSelectDialog.IFileSelection, TextWatcher, AdapterView.OnItemSelectedListener {
    private ImageView apkIcon;
    private ApkInfoParser.AppInfo apkInfo;
    private TextView apkLabel;
    private String apkPath;
    private EditText appNameEt;
    private Map<String, String> extraStrReplaces;
    private MyHandler handler = new MyHandler(this);
    private Spinner installLocSpinner;
    private ImageView launcherIcon;
    private String launcherIconPath;
    private ManifestInfo manifestInfo;
    private ManifestParser manifestParser;
    private EditText maxSdkEt;
    private TextView maxSdkLabel;
    private EditText minSdkEt;
    private TextView minSdkLabel;
    private String newAppName;
    private int newInstallLocation;
    private String newManifestFile;
    private String newPackageName;
    private int newVerCode;
    private String newVerName;
    private EditText pkgNameEt;
    private Map<String, String> refactored;
    private CheckBox renameDexCb;
    private View renameExtraLayout;
    private Spinner renameMethodSpinner;
    private CheckBox renameResCb;
    private EditText targetSdkEt;
    private TextView targetSdkLabel;
    private EditText verCodeEt;
    private EditText verNameEt;

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        setContentView(R.layout.activity_commonedit);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");
        new Thread() {
            @Override
            public void run() {
                try {
                    CommonEditActivity.this.apkInfo = new ApkInfoParser().parse(CommonEditActivity.this, CommonEditActivity.this.apkPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    ZipFile zipFile = new ZipFile(CommonEditActivity.this.apkPath);
                    ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
                    InputStream is = zipFile.getInputStream(entry);
                    CommonEditActivity.this.manifestParser = new ManifestParser(is);
                    CommonEditActivity.this.manifestParser.parse();
                    is.close();
                    zipFile.close();
                    ManifestInfo mfInfo = CommonEditActivity.this.manifestParser.getManifestInfo();
                    if (CommonEditActivity.this.apkInfo == null) {
                        CommonEditActivity.this.apkInfo = new ApkInfoParser.AppInfo();
                        ApkParser parser = new ApkParser(CommonEditActivity.this, CommonEditActivity.this.apkPath);
                        parser.parse(mfInfo.appNameId, mfInfo.launcherId);
                        CommonEditActivity.this.apkInfo.label = parser.getAppName();
                        Bitmap bitmap = parser.getAppIcon();
                        if (bitmap != null) {
                            CommonEditActivity.this.apkInfo.icon = new BitmapDrawable(bitmap);
                        }
                    }
                    CommonEditActivity.this.handler.sendEmptyMessage(0);
                } catch (Exception e2) {
                    e2.printStackTrace();
                    CommonEditActivity.this.handler.setErrorMessage(e2.getMessage());
                    CommonEditActivity.this.handler.sendEmptyMessage(1);
                }
            }
        }.start();
        initView();
    }

    public void parseFinished(String errMessage) {
        if (errMessage == null) {
            this.manifestInfo = this.manifestParser.getManifestInfo();
            this.manifestInfo.appName = this.apkInfo.label;
            this.apkIcon.setImageDrawable(this.apkInfo.icon);
            this.apkLabel.setText(this.apkInfo.label);
            this.appNameEt.setText(this.apkInfo.label);
            this.pkgNameEt.setText(this.manifestInfo.packageName);
            this.renameExtraLayout.setVisibility(View.GONE);
            if (this.manifestInfo.verNameIdx == -1) {
                this.verNameEt.setVisibility(8);
                findViewById(R.id.tv_vername).setVisibility(8);
            } else {
                this.verNameEt.setText(this.manifestInfo.versionName);
            }
            this.verCodeEt.setText(String.valueOf(this.manifestInfo.versionCode));
            int location = this.manifestInfo.installLocation;
            if (location >= -1 && location < 3) {
                this.installLocSpinner.setSelection(location + 1);
            }
            this.launcherIcon.setImageDrawable(this.apkInfo.icon);
            if (this.manifestInfo.launcherId != -1) {
                this.launcherIcon.setOnClickListener(this);
            }
            updateSdkVersion(this.minSdkLabel, this.minSdkEt, this.manifestInfo.minSdkVersion);
            updateSdkVersion(this.targetSdkLabel, this.targetSdkEt, this.manifestInfo.targetSdkVersion);
            updateSdkVersion(this.maxSdkLabel, this.maxSdkEt, this.manifestInfo.maxSdkVersion);
            return;
        }
        String msg = getResources().getString(R.string.cannot_parse_apk);
        Toast.makeText((Context) this, (CharSequence) (msg + ": " + errMessage), 1).show();
        finish();
    }

    private void updateSdkVersion(TextView label, EditText editText, int sdkVersion) {
        if (sdkVersion != -1) {
            editText.setText(String.valueOf(sdkVersion));
            label.setVisibility(0);
            editText.setVisibility(0);
        } else {
            label.setVisibility(8);
            editText.setVisibility(8);
        }
    }

    private void initView() {
        this.apkIcon = (ImageView) findViewById(R.id.apk_icon);
        this.launcherIcon = (ImageView) findViewById(R.id.launcher_icon);
        this.apkLabel = (TextView) findViewById(R.id.apk_label);
        this.minSdkLabel = (TextView) findViewById(R.id.tv_minSdkVersion);
        this.targetSdkLabel = (TextView) findViewById(R.id.tv_targetSdkVersion);
        this.maxSdkLabel = (TextView) findViewById(R.id.tv_maxSdkVersion);
        this.appNameEt = (EditText) findViewById(R.id.et_appname);
        this.pkgNameEt = (EditText) findViewById(R.id.et_pkgname);
        this.verCodeEt = (EditText) findViewById(R.id.et_vercode);
        this.verNameEt = (EditText) findViewById(R.id.et_vername);
        this.minSdkEt = (EditText) findViewById(R.id.et_minSdkVersion);
        this.targetSdkEt = (EditText) findViewById(R.id.et_targetSdkVersion);
        this.maxSdkEt = (EditText) findViewById(R.id.et_maxSdkVersion);
        this.renameResCb = (CheckBox) findViewById(R.id.cb_rename_resource);
        this.renameDexCb = (CheckBox) findViewById(R.id.cb_rename_dex);
        this.renameExtraLayout = findViewById(R.id.layout_rename_extra);
        this.renameMethodSpinner = (Spinner) findViewById(R.id.rename_method_spinner);
        // This fork ships the full editor as ru.apkeditor.plus. The old suffix check
        // disabled package editing simply because the applicationId no longer ends in "pro".
        boolean canRenamePackage = BuildConfig.IS_PRO;
        this.pkgNameEt.setEnabled(canRenamePackage);
        this.renameMethodSpinner.setEnabled(canRenamePackage);
        this.renameResCb.setEnabled(canRenamePackage);
        this.renameDexCb.setEnabled(canRenamePackage);
        this.pkgNameEt.addTextChangedListener(this);
        this.pkgNameEt.setOnFocusChangeListener((field, focused) -> {
            if (!focused) {
                String normalized = PackageNameValidator.normalize(this.pkgNameEt.getText().toString());
                if (!normalized.equals(this.pkgNameEt.getText().toString())) {
                    this.pkgNameEt.setText(normalized);
                }
                updatePackageError();
            }
        });
        this.installLocSpinner = (Spinner) findViewById(R.id.location_spinner);
        Resources res = getResources();
        String[] strLocations = {res.getString(R.string.location_0), res.getString(R.string.location_1), res.getString(R.string.location_2), res.getString(R.string.location_3)};
        ArrayAdapter<String> installLocAdapter = new ArrayAdapter<>((Context) this, android.R.layout.simple_spinner_item, strLocations);
        installLocAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.installLocSpinner.setAdapter((SpinnerAdapter) installLocAdapter);
        String[] strRenameMds = {res.getString(R.string.pkg_rename_direct), res.getString(R.string.pkg_rename_as_plugin)};
        ArrayAdapter<String> renameMethodAdapter = new ArrayAdapter<>((Context) this, android.R.layout.simple_spinner_item, strRenameMds);
        renameMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.renameMethodSpinner.setAdapter((SpinnerAdapter) renameMethodAdapter);
        this.renameMethodSpinner.setOnItemSelectedListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
            return;
        }
        if (id == R.id.btn_save) {
            saveChanges();
        } else if (id == R.id.launcher_icon) {
            FileSelectDialog dlg = new FileSelectDialog(this, this, ".png", "launcher.png", null);
            dlg.show();
        }
    }

    private void extractAssets(String assetName, String filePath) throws IOException {
        InputStream input = getAssets().open(assetName);
        FileOutputStream output = new FileOutputStream(filePath);
        IOUtils.copy(input, output);
        input.close();
        output.close();
    }

    private void enhancedPackageRename() {
        File fileDir = getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String tmplApkPath = rootDirectory + "/bin/_wrapper.apk";
        File binDir = new File(rootDirectory + "/bin");
        if (!binDir.exists() && !binDir.mkdirs()) {
            Toast.makeText((Context) this, (CharSequence) "Cannot create bin directory", 1).show();
            return;
        }
        String authString = RandomUtil.getRandomString(4);
        try {
            this.newManifestFile = SDCard.makeWorkingDir(this) + ".xml";
            extractWrapperIfNeeded(tmplApkPath);
            ZipFile zipFile = new ZipFile(tmplApkPath);
            ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
            InputStream is = zipFile.getInputStream(entry);
            PluginWrapperMfEditor editor = new PluginWrapperMfEditor(is, this.newManifestFile);
            editor.replaceString("com.example.droidpluginwrapper", this.newPackageName);
            editor.replaceString("DroidPluginWrapper", this.newAppName);
            editor.replaceString("1.x.y", this.newVerName);
            for (int i = 0; i < 9; i++) {
                char c = (char) (i + 48);
                editor.replaceString("com.morgoo.droidplugin_stub_P0" + c, "com.morgoo.droidplugin_" + authString + "_P0" + c);
            }
            editor.setVersionCode(this.newVerCode);
            editor.setInstallLocation(this.newInstallLocation < 0 ? 0 : this.newInstallLocation);
            if (!this.manifestInfo.permissions.isEmpty()) {
                List<String> permissions = new ArrayList<>();
                for (String permName : this.manifestInfo.permissions) {
                    permissions.add(permName);
                }
                editor.addPermissions(permissions);
            }
            editor.saveModification();
            is.close();
            zipFile.close();
            Map<String, String> fileReplaces = new HashMap<>();
            fileReplaces.put("AndroidManifest.xml", this.newManifestFile);
            fileReplaces.put("assets/1.apk", this.apkPath);
            if (this.launcherIconPath != null) {
                fileReplaces.put("res/drawable/ic_launcher.png", this.launcherIconPath);
            } else {
                String path = saveLauncher();
                if (path != null) {
                    fileReplaces.put("res/drawable/ic_launcher.png", path);
                }
            }
            Intent intent = new Intent((Context) this, (Class<?>) ApkCreateActivity.class);
            ActivityUtil.attachParam(intent, "apkPath", tmplApkPath);
            ActivityUtil.attachParam(intent, "packageName", this.manifestInfo.packageName);
            ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
            ArrayList<IApkMaking> extraTasks = new ArrayList<>();
            extraTasks.add(new PluginWrapperExtra(authString));
            intent.putExtra("interfaces", extraTasks);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText((Context) this, (CharSequence) e.getMessage(), 1).show();
            e.printStackTrace();
        }
    }

    private void extractWrapperIfNeeded(String tmplApkPath) throws IOException {
        String curVersion = null;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        SharedPreferences sp = getSharedPreferences("info", 0);
        String lastVersion = sp.getString("wrapper_ver", "");
        if (!new File(tmplApkPath).exists() || !lastVersion.equals(curVersion)) {
            try {
                extractAssets("_wrapper", tmplApkPath);
                FileEncrypter.encrypt(tmplApkPath);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("wrapper_ver", curVersion);
                editor.apply();
            } catch (Exception e2) {
            }
        }
    }

    private String saveLauncher() {
        try {
            if (this.apkInfo.icon != null) {
                String file = SDCard.makeWorkingDir(this) + "_launcher";
                Bitmap bm = drawableToBitmap(this.apkInfo.icon);
                FileOutputStream outStream = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
                return file;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveChanges() {
        boolean notModified;
        boolean notModified2;
        boolean notModified3;
        boolean notModified4;
        boolean z;
        boolean z2;
        String strCode = this.verCodeEt.getText().toString();
        this.newVerCode = 0;
        try {
            this.newVerCode = Integer.valueOf(strCode).intValue();
            this.newAppName = this.appNameEt.getText().toString();
            this.newPackageName = PackageNameValidator.normalize(this.pkgNameEt.getText().toString());
            if (!this.newPackageName.equals(this.pkgNameEt.getText().toString())) {
                this.pkgNameEt.setText(this.newPackageName);
            }
            this.newVerName = this.verNameEt.getText().toString();
            int minSdkVersion = -1;
            int targetSdkVersion = -1;
            int maxSdkVersion = -1;
            try {
                String str = this.minSdkEt.getText().toString();
                if (!"".equals(str)) {
                    minSdkVersion = Integer.valueOf(str).intValue();
                }
                String str2 = this.targetSdkEt.getText().toString();
                if (!"".equals(str2)) {
                    targetSdkVersion = Integer.valueOf(str2).intValue();
                }
                String str3 = this.maxSdkEt.getText().toString();
                if (!"".equals(str3)) {
                    maxSdkVersion = Integer.valueOf(str3).intValue();
                }
            } catch (Exception e) {
            }
            if (!checkPackageName()) {
                return;
            }
            int selection = this.installLocSpinner.getSelectedItemPosition();
            this.newInstallLocation = selection - 1;
            if (this.launcherIconPath != null || !this.manifestInfo.appName.equals(this.newAppName) || !this.manifestInfo.packageName.equals(this.newPackageName) || this.manifestInfo.versionCode != this.newVerCode || this.manifestInfo.installLocation != this.newInstallLocation) {
                notModified = false;
            } else {
                notModified = true;
            }
            if (this.manifestInfo.verNameIdx != -1) {
                if (!notModified || !this.manifestInfo.versionName.equals(this.newVerName)) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                notModified = z2;
            }
            if (!notModified || this.manifestInfo.minSdkVersion != minSdkVersion) {
                notModified2 = false;
            } else {
                notModified2 = true;
            }
            if (!notModified2 || this.manifestInfo.targetSdkVersion != targetSdkVersion) {
                notModified3 = false;
            } else {
                notModified3 = true;
            }
            if (!notModified3 || this.manifestInfo.maxSdkVersion != maxSdkVersion) {
                notModified4 = false;
            } else {
                notModified4 = true;
            }
            if (notModified4) {
                Toast.makeText((Context) this, R.string.no_change_detected, 1).show();
                return;
            }
            int renameMethodIdx = this.renameMethodSpinner.getSelectedItemPosition();
            if (!this.manifestInfo.packageName.equals(this.newPackageName) && renameMethodIdx == 1) {
                enhancedPackageRename();
                return;
            }
            if (!this.renameDexCb.isChecked() || this.manifestInfo.packageName.equals(this.newPackageName)) {
                z = false;
            } else {
                z = true;
            }
            boolean refactorDex = z;
            try {
                this.newManifestFile = SDCard.makeWorkingDir(this) + ".xml";
                ZipFile zipFile = new ZipFile(this.apkPath);
                ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
                InputStream is = zipFile.getInputStream(entry);
                ManifestInfo newInfo = new ManifestInfo();
                newInfo.appName = this.newAppName;
                newInfo.packageName = this.newPackageName;
                newInfo.versionCode = this.newVerCode;
                newInfo.versionName = this.newVerName;
                newInfo.installLocation = this.newInstallLocation;
                newInfo.minSdkVersion = minSdkVersion;
                newInfo.targetSdkVersion = targetSdkVersion;
                newInfo.maxSdkVersion = maxSdkVersion;
                ManifestEditorNew editor = new ManifestEditorNew(is, this.newManifestFile, refactorDex);
                editor.modify(this.manifestInfo, newInfo);
                editor.save();
                this.refactored = editor.getRefactoredNames();
                this.extraStrReplaces = new HashMap();
                is.close();
                zipFile.close();
                makeAPK(false, refactorDex);
            } catch (Exception e2) {
                Toast.makeText((Context) this, (CharSequence) e2.getMessage(), 1).show();
                e2.printStackTrace();
            }
        } catch (Exception e3) {
            Toast.makeText((Context) this, R.string.invalid_ver_code, 1).show();
        }
    }

    private void updatePackageError() {
        String original = this.manifestInfo == null ? null : this.manifestInfo.packageName;
        this.pkgNameEt.setError(PackageNameValidator.validationError(this.pkgNameEt.getText().toString(), original));
    }

    private boolean checkPackageName() {
        updatePackageError();
        String error = PackageNameValidator.validationError(this.newPackageName,
                this.manifestInfo == null ? null : this.manifestInfo.packageName);
        if (error != null) {
            this.pkgNameEt.requestFocus();
            return false;
        }
        return true;
    }

    private void makeAPK(boolean bCheatPkgName, boolean refactorDex) {
        Map<String, String> fileReplaces = new HashMap<>();
        fileReplaces.put("AndroidManifest.xml", this.newManifestFile);
        Intent intent = new Intent((Context) this, (Class<?>) ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtil.attachParam(intent, "packageName", this.manifestInfo.packageName);
        ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
        if (!this.manifestInfo.packageName.equals(this.newPackageName) && this.renameResCb.isChecked()) {
            ActivityUtil.attachParam(intent, "newPackageNameInArsc", this.newPackageName);
        }
        if (this.manifestInfo.appNameIdx < 0 && !this.newAppName.equals(this.manifestInfo.appName)) {
            ActivityUtil.attachParam(intent, "oldAppNameInArsc", this.manifestInfo.appName);
            ActivityUtil.attachParam(intent, "newAppNameInArsc", this.newAppName);
        }
        if (!this.refactored.isEmpty()) {
            Map<String, String> replaces = new HashMap<>();
            for (Map.Entry<String, String> entry : this.refactored.entrySet()) {
                String key = entry.getKey();
                String key2 = "L" + key.replaceAll("\\.", "/");
                String value = entry.getValue();
                replaces.put(key2, "L" + value.replaceAll("\\.", "/"));
            }
            ActivityUtil.attachParam(intent, "classRenames", replaces);
        }
        ArrayList<IApkMaking> extraTasks = new ArrayList<>();
        if (this.manifestInfo.launcherId != -1 && this.launcherIconPath != null) {
            ReplaceLauncherIcon task = new ReplaceLauncherIcon(this.manifestInfo.launcherId, this.launcherIconPath);
            extraTasks.add(task);
        }
        if (refactorDex) {
            RefactorDex task2 = new RefactorDex(this.manifestInfo.packageName, this.newPackageName);
            extraTasks.add(task2);
            RefactorLayout t = new RefactorLayout(this.manifestInfo.packageName, this.newPackageName);
            extraTasks.add(t);
        }
        String oldPackageName = this.manifestInfo.packageName;
        if (bCheatPkgName && !oldPackageName.equals(this.newPackageName)) {
            extraTasks.add(new DexDecode());
            extraTasks.add(new DexPatch_CheatPkgName(this.manifestInfo.applicationCls, oldPackageName));
            extraTasks.add(new DexEncode());
        }
        if (!extraTasks.isEmpty()) {
            intent.putExtra("interfaces", extraTasks);
        }
        startActivity(intent);
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        this.launcherIconPath = filePath;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(this.launcherIconPath);
            this.launcherIcon.setImageBitmap(bitmap);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return filename.endsWith(".png");
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String newPkgName = PackageNameValidator.normalize(s.toString());
        if (this.manifestInfo != null && newPkgName.equals(this.manifestInfo.packageName)) {
            this.renameExtraLayout.setVisibility(View.GONE);
        } else {
            this.renameExtraLayout.setVisibility(View.VISIBLE);
        }
        if (this.pkgNameEt.hasFocus()) {
            updatePackageError();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 1) {
            this.renameResCb.setVisibility(8);
            this.renameDexCb.setVisibility(8);
        } else {
            this.renameResCb.setVisibility(0);
            this.renameDexCb.setVisibility(0);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private static class MyHandler extends Handler {
        private WeakReference<CommonEditActivity> actRef;
        private String errMessage;

        MyHandler(CommonEditActivity act) {
            this.actRef = new WeakReference<>(act);
        }

        void setErrorMessage(String errMessage) {
            this.errMessage = errMessage;
        }

        @Override
        public void handleMessage(Message msg) {
            CommonEditActivity act = this.actRef.get();
            if (act == null) {
            }
            switch (msg.what) {
                case 0:
                    act.parseFinished(null);
                    break;
                case 1:
                    act.parseFinished(this.errMessage);
                    break;
            }
        }
    }
}
