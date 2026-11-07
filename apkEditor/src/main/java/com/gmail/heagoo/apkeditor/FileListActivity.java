package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.LruCache;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.EditModeDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.apkeditor.util.DebugDialog;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.TextFileReader;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.FolderListWrapper;
import com.gmail.heagoo.folderlist.IListEventListener;
import com.gmail.heagoo.folderlist.IListItemProducer;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FileListActivity extends CustomizedLangActivity implements IListEventListener, IListItemProducer, EditModeDialog.IEditModeSelected, View.OnClickListener {
    private static final String LAST_DIR_KEY = "apkDirectory";
    private FolderListWrapper foderWrapper;
    private boolean isDarkTheme;
    private ListView listView;
    private TextView pathTV;
    private ImageView sdCardImg;
    private View switchView;
    private boolean showingExtSdCard = false;
    private ApkParseThread parseThread = null;
    private PopupWindow popupWindow = null;
    private LruCache<String, ApkInfoParser.AppInfo> apkIconCache = new LruCache<>(64);
    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    FileListActivity.this.foderWrapper.getAdapter().notifyDataSetChanged();
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setContentView(R.layout.activity_listfile);
        if (this.parseThread == null) {
            this.parseThread = new ApkParseThread();
            this.parseThread.start();
        }
        initWithPermChecking();
    }

    public void onDestroy() {
        if (this.parseThread != null) {
            this.parseThread.stopParse();
        }
        super.onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length <= 0 || grantResults[0] != 0 || this.foderWrapper != null) {
            return;
        }
        init();
    }

    public void onResume() {
        super.onResume();
        if (this.foderWrapper != null || !StorageAccess.has(this)) {
            return;
        }
        init();
    }

    private void initWithPermChecking() {
        if (!StorageAccess.has(this)) {
            StorageAccess.request(this, this.WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else if (this.foderWrapper == null) {
            init();
        }
    }

    private void init() {
        String curDir = getLastDirectory();
        this.pathTV = (TextView) findViewById(R.id.dirPath);
        this.pathTV.setText(curDir);
        this.listView = (ListView) findViewById(R.id.file_list);
        this.foderWrapper = new FolderListWrapper(this, this.listView, curDir, "/", this, this);
        SharedPreferences sorting = getSharedPreferences("config", 0);
        this.foderWrapper.getAdapter().setSort(sorting.getInt("file_sort_key", 0), sorting.getBoolean("file_sort_desc", false));
        findViewById(R.id.sort_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FileListActivity.this.m204lambda$init$0$comgmailheagooapkeditorFileListActivity(view);
            }
        });
        this.sdCardImg = (ImageView) findViewById(R.id.imageView_extsdcard);
        this.switchView = findViewById(R.id.menu_switch_card);
        this.switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    FileListActivity.this.switchSdCard();
                } catch (Exception e) {
                }
            }
        });
        this.switchView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FileListActivity.this.showStorageList(v);
                return true;
            }
        });
        Button searchBtn = (Button) findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);
    }

    void m204lambda$init$0$comgmailheagooapkeditorFileListActivity(View v) {
        showSortDialog();
    }

    private void showSortDialog() {
        final SharedPreferences sharedPreferences = getSharedPreferences("config", 0);
        int i = sharedPreferences.getInt("file_sort_key", 0);
        if (i < 0 || i > 3) {
            i = 0;
        }
        new AlertDialog.Builder(this).setTitle(R.string.file_sort_title).setSingleChoiceItems(R.array.file_sort_options, (i * 2) + (sharedPreferences.getBoolean("file_sort_desc", false) ? 1 : 0), new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i2) {
                FileListActivity.this.m205x39adede5(sharedPreferences, dialogInterface, i2);
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    void m205x39adede5(SharedPreferences prefs, DialogInterface dialog, int which) {
        int selectedKey = which / 2;
        boolean descending = which % 2 != 0;
        prefs.edit().putInt("file_sort_key", selectedKey).putBoolean("file_sort_desc", descending).apply();
        this.foderWrapper.getAdapter().setSort(selectedKey, descending);
        this.listView.setSelection(0);
        dialog.dismiss();
    }

    public void showStorageList(View parent) {
        int height;
        if (this.popupWindow == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService("layout_inflater");
            View view = layoutInflater.inflate(R.layout.popup_list, (ViewGroup) null);
            ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);
            ArrayList<String> groups = new ArrayList<>();
            Resources res = getResources();
            groups.add(res.getString(R.string.sd_card));
            groups.add(res.getString(R.string.ext_sd_card));
            groups.add(res.getString(R.string.app_files));
            ArrayAdapter<String> adapter = new ArrayAdapter<>((Context) this, android.R.layout.simple_list_item_1, (List<String>) groups);
            lv_group.setAdapter((ListAdapter) adapter);
            view.measure(0, 0);
            int width = (Display.getWidth(this) * 2) / 5;
            int height2 = view.getMeasuredHeight();
            if (height2 == 0) {
                height = Display.dip2px(this, 150.0f);
            } else {
                height = (height2 * 3) + Display.dip2px(this, 2.0f);
            }
            this.popupWindow = new PopupWindow(view, width, height);
            lv_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view2, int position, long id) {
                    switch (position) {
                        case 0:
                            FileListActivity.this.openSdCard();
                            break;
                        case 1:
                            FileListActivity.this.openExtSdCard();
                            break;
                        case 2:
                            FileListActivity.this.openAppFiles();
                            break;
                    }
                    if (FileListActivity.this.popupWindow != null) {
                        FileListActivity.this.popupWindow.dismiss();
                    }
                }
            });
        }
        this.popupWindow.setFocusable(true);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService("window");
        int xPos = (windowManager.getDefaultDisplay().getWidth() / 2) - (this.popupWindow.getWidth() / 2);
        this.popupWindow.showAsDropDown(parent, xPos, 0);
    }

    public void switchSdCard() {
        boolean switched = false;
        if (this.showingExtSdCard) {
            openSdCard();
            this.sdCardImg.setImageResource(R.drawable.lab_storage);
            switched = true;
        } else if (openExtSdCard()) {
            this.sdCardImg.setImageResource(R.drawable.lab_storage);
            switched = true;
        }
        if (switched) {
            this.showingExtSdCard = !this.showingExtSdCard;
        }
    }

    public boolean openExtSdCard() {
        String path = SDCard.getExternalStoragePath(this);
        if (path != null && !path.equals("")) {
            if (this.foderWrapper == null) {
                return false;
            }
            this.foderWrapper.openDirectory(path);
            return true;
        }
        Toast.makeText((Context) this, R.string.cannot_find_ext_sdcard, 0).show();
        return false;
    }

    public boolean openAppFiles() {
        File f = getFilesDir();
        if (this.foderWrapper != null) {
            this.foderWrapper.openDirectory(f.getPath());
            return true;
        }
        return false;
    }

    private void showDebugDialog() {
        DebugDialog dialog = new DebugDialog(this);
        dialog.show();
        try {
            TextFileReader reader = new TextFileReader("/proc/mounts");
            dialog.addLog(reader.getContents());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openSdCard() {
        String path = SDCard.getRootDirectory();
        if (path != null && !path.equals("") && this.foderWrapper != null) {
            this.foderWrapper.openDirectory(path);
        }
    }

    public void dirChanged(String newDir) {
        if (this.pathTV != null) {
            this.pathTV.setText(newDir);
        }
    }

    public void fileRenamed(String dirPath, String oldName, String newName) {
    }

    public void fileDeleted(String dirPath, String fileName) {
    }

    public void itemLongClicked(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    }

    public boolean fileClicked(String filePath) {
        if (!filePath.endsWith(".apk")) {
            return false;
        }
        String directory = filePath.substring(0, filePath.lastIndexOf(47));
        setLastDirectory(directory);
        if (!MainActivity.upgradedFromOldVersion(this)) {
            startFullEditActivity(filePath);
            return true;
        }
        EditModeDialog dlg = new EditModeDialog(this, this, filePath);
        dlg.show();
        return true;
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

    private String getLastDirectory() {
        String rootDir = SDCard.getRootDirectory();
        SharedPreferences sp = getSharedPreferences("config", 0);
        return sp.getString(LAST_DIR_KEY, rootDir);
    }

    private void setLastDirectory(String directory) {
        SharedPreferences sp = getSharedPreferences("config", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(LAST_DIR_KEY, directory);
        editor.commit();
    }

    public void updateList() {
        this.handler.removeMessages(0);
        this.handler.sendEmptyMessageDelayed(0, 300L);
    }

    public Drawable getFileIcon(String dirPath, FileRecord record) {
        if (record == null || record.isDir || !record.fileName.endsWith(".apk")) {
            return null;
        }
        String path = dirPath + "/" + record.fileName;
        ApkInfoParser.AppInfo info = this.apkIconCache.get(path);
        if (info != null) {
            return info.icon;
        }
        this.parseThread.addApk(path);
        return getResources().getDrawable(R.drawable.lab_apk_file);
    }

    public String getDetail1(String dirPath, FileRecord record) {
        ApkInfoParser.AppInfo info;
        if (record == null || record.isDir) {
            return null;
        }
        String detail = Formatter.formatFileSize(this, Math.max(0L, record.totalSize));
        if (record.lastModified > 0) {
            detail = detail + "  ·  " + DateFormat.getDateFormat(this).format(new Date(record.lastModified));
        }
        return (!record.fileName.toLowerCase(Locale.ROOT).endsWith(".apk") || (info = this.apkIconCache.get(new StringBuilder().append(dirPath).append("/").append(record.fileName).toString())) == null || info.label == null || info.label.toString().isEmpty()) ? detail : info.label + "  ·  " + detail;
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.search_button && this.foderWrapper != null) {
            EditText et = (EditText) findViewById(R.id.keyword_edit);
            String keyword = et.getText().toString();
            String currentFolder = this.foderWrapper.getAdapter().getData((List) null);
            Intent intent = new Intent((Context) this, (Class<?>) ApkSearchActivity.class);
            ActivityUtil.attachParam(intent, "Keyword", keyword);
            ActivityUtil.attachParam(intent, "Path", currentFolder);
            startActivity(intent);
        }
    }

    class ApkParseThread extends Thread {
        private ApkInfoParser apkParser = null;
        private boolean bStop = false;
        private List<String> apkList = new LinkedList();

        ApkParseThread() {
        }

        public void addApk(String path) {
            synchronized (this.apkList) {
                this.apkList.add(path);
                this.apkList.notify();
            }
        }

        public void stopParse() {
            this.bStop = true;
            synchronized (this.apkList) {
                this.apkList.notify();
            }
        }

        @Override
        public void run() {
            String path;
            this.apkParser = new ApkInfoParser();
            while (!this.bStop) {
                synchronized (this.apkList) {
                    if (this.apkList.isEmpty()) {
                        try {
                            this.apkList.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    if (!this.apkList.isEmpty()) {
                        path = this.apkList.remove(0);
                    } else {
                        path = null;
                    }
                }
                if (path != null) {
                    ApkInfoParser.AppInfo info = null;
                    try {
                        info = this.apkParser.parse(FileListActivity.this, path);
                    } catch (Throwable th) {
                    }
                    if (info != null) {
                        FileListActivity.this.apkIconCache.put(path, info);
                        FileListActivity.this.updateList();
                    }
                }
            }
        }
    }
}
