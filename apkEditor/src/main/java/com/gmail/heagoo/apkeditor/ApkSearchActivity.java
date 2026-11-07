package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.EditModeDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkSearchActivity extends CustomizedLangActivity implements AdapterView.OnItemClickListener, EditModeDialog.IEditModeSelected {
    private List<String> apkFileList = new ArrayList();
    private ApkListAdapter apkListAdapter;
    private String keyword;
    private String searchPath;
    private View searchingLayout;
    private TextView titleTV;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setContentView(R.layout.activity_apksearch);
        Intent intent = getIntent();
        this.keyword = ActivityUtil.getParam(intent, "Keyword");
        this.searchPath = ActivityUtil.getParam(intent, "Path");
        initView();
        new ApkSearchThread().start();
    }

    private void initView() {
        this.titleTV = (TextView) findViewById(R.id.title);
        this.searchingLayout = findViewById(R.id.searching_layout);
        ListView apkListView = (ListView) findViewById(R.id.listview_apkfiles);
        this.apkListAdapter = new ApkListAdapter(this);
        apkListView.setAdapter((ListAdapter) this.apkListAdapter);
        apkListView.setOnItemClickListener(this);
        setTitleText(0);
    }

    public void foundApkFile(final String apkPath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ApkSearchActivity.this.apkFileList.add(apkPath);
                ApkSearchActivity.this.setTitleText(ApkSearchActivity.this.apkFileList.size());
                ApkSearchActivity.this.apkListAdapter.addApkFile(apkPath);
            }
        });
    }

    public void setTitleText(int apkNum) {
        String title = String.format(getString(R.string.str_files_found), Integer.valueOf(apkNum), this.keyword);
        if ("".equals(this.keyword)) {
            title = title.substring(0, title.length() - 4);
        }
        this.titleTV.setText(title);
    }

    public void apkSearchDone() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ApkSearchActivity.this.searchingLayout.setVisibility(8);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        if (position < this.apkFileList.size()) {
            String filePath = this.apkFileList.get(position);
            if (!MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(filePath);
            } else {
                EditModeDialog dlg = new EditModeDialog(this, this, filePath);
                dlg.show();
            }
        }
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

    @Override
    public void editModeSelected(int mode, String extraStr) {
        Intent intent = null;
        switch (mode) {
            case 0:
                startFullEditActivity(extraStr);
                return;
            case 1:
                intent = new Intent((Context) this, (Class<?>) SimpleEditActivity.class);
                break;
            case 2:
                intent = new Intent((Context) this, (Class<?>) CommonEditActivity.class);
                break;
            case 4:
                intent = new Intent((Context) this, (Class<?>) AxmlEditActivity.class);
                break;
        }
        if (intent != null) {
            ActivityUtil.attachParam(intent, "apkPath", extraStr);
            startActivity(intent);
        }
    }

    class ApkSearchThread extends Thread {
        String lcKey;

        ApkSearchThread() {
            this.lcKey = ApkSearchActivity.this.keyword.toLowerCase();
        }

        @Override
        public void run() {
            searchInFolder(new File(ApkSearchActivity.this.searchPath));
            ApkSearchActivity.this.apkSearchDone();
        }

        private void searchInFolder(File curDir) {
            File[] files = curDir.listFiles();
            if (files != null) {
                List<File> dirList = new ArrayList<>();
                for (File f : files) {
                    if (f.isFile()) {
                        String fileName = f.getName();
                        if (fileName.endsWith(".apk") && fileName.toLowerCase().contains(this.lcKey)) {
                            ApkSearchActivity.this.foundApkFile(f.getAbsolutePath());
                        }
                    } else {
                        dirList.add(f);
                    }
                }
                for (File dir : dirList) {
                    searchInFolder(dir);
                }
            }
        }
    }
}
