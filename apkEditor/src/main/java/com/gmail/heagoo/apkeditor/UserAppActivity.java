package com.gmail.heagoo.apkeditor;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.DecodeModeDialog;
import com.gmail.heagoo.apkeditor.EditModeDialog;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.AppListOrder;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserAppActivity extends CustomizedLangActivity implements AdapterView.OnItemClickListener, View.OnClickListener, EditModeDialog.IEditModeSelected {
    private ListView appListView;
    private EditText keywordEdit;
    private GroupAdapter popListAdapter;
    private PopupWindow popupWindow;
    private ProgressBar progressBar;
    private Button searchBtn;
    private final List<AppInfo> displayApps = new ArrayList();
    private final int APPINFO_ID = 2;
    private final int BACKUP_ID = 3;
    private final int LAUNCH_ID = 4;
    private int appTypeIndex = 0;
    private int appOrderIndex = 0;
    private int scanGeneration;
    private MyHandler handler = new MyHandler(this);
    private List<AppInfo> allApps = new ArrayList();

    public static boolean startFullEditActivity(final Context ctx, final String filePath) {
        String mode = SettingActivity.getDecodeMode(ctx);
        if ("2".equals(mode)) {
            new DecodeModeDialog(ctx, new DecodeModeDialog.IDecodeModeSelected() {
                @Override
                public final void decodeModeSelected(int i, String str) {
                    UserAppActivity.startFullEditActivity(ctx, filePath, "" + i);
                }
            }, filePath).show();
            return false;
        }
        startFullEditActivity(ctx, filePath, mode);
        return true;
    }

    public static void startFullEditActivity(Context ctx, String filePath, String mode) {
        RecentFiles.record(ctx, filePath, null);
        Intent intent = new Intent(ctx, (Class<?>) ApkInfoExActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", filePath);
        boolean fullDecoding = "0".equals(mode);
        ActivityUtil.attachBoolParam(intent, "isFullDecoding", fullDecoding);
        ctx.startActivity(intent);
    }

    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
            }
            i.addCategory("android.intent.category.LAUNCHER");
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        int layoutId = R.layout.activity_applist;
        setContentView(layoutId);
        String strOrder = SettingActivity.getListOrder(this);
        this.appOrderIndex = getOrderIndex(strOrder);
        initUI();
    }

    public void onResume() {
        super.onResume();
        this.appOrderIndex = getOrderIndex(SettingActivity.getListOrder(this));
        reScanAppList();
    }

    public void showAppList(List<AppInfo> apps) {
        this.progressBar.setVisibility(8);
        this.appListView.setVisibility(0);
        AppListAdapter adapter = (AppListAdapter) this.appListView.getAdapter();
        AppListOrder order = AppListOrder.fromIndex(this.appOrderIndex);
        adapter.setAppList(apps, order.getPreferenceValue());
        List<AppInfo> sorted = adapter.getAppList();
        synchronized (this.displayApps) {
            this.displayApps.clear();
            this.displayApps.addAll(sorted);
        }
        adapter.notifyDataSetChanged();
        this.keywordEdit.setEnabled(true);
        this.searchBtn.setEnabled(true);
    }

    private int getOrderIndex(String strOrder) {
        return AppListOrder.fromPreference(strOrder,
                getResources().getStringArray(R.array.order_key)).ordinal();
    }

    protected void initAppList() {
        AppListAdapter adapter = new AppListAdapter(this);
        adapter.setAppList(this.displayApps, SettingActivity.getListOrder(this));
        this.appListView.setAdapter((ListAdapter) adapter);
        this.appListView.setOnItemClickListener(this);
        registerForContextMenu(this.appListView);
    }

    private void initUI() {
        this.progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        TextView tv = (TextView) findViewById(R.id.apptype);
        tv.setText(R.string.select_apk_from_app);
        View moreMenu = findViewById(R.id.menu_more);
        moreMenu.setOnClickListener(this);
        this.appListView = (ListView) findViewById(R.id.application_list);
        initAppList();
        this.keywordEdit = (EditText) findViewById(R.id.et_keyword);
        this.searchBtn = (Button) findViewById(R.id.btn_search);
        this.searchBtn.setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
    }

    private void reScanAppList(int typeIndex, int orderIndex) {
        this.appTypeIndex = typeIndex;
        this.appOrderIndex = orderIndex;
        reScanAppList();
    }

    private void reScanAppList() {
        final int generation = ++this.scanGeneration;
        final int typeIndex = this.appTypeIndex;
        this.allApps.clear();
        new Thread() {
            @Override
            public void run() {
                List<AppInfo> appList = new ArrayList<>();
                UserAppActivity.this.getAppList(appList, typeIndex);
                UserAppActivity.this.handler.obtainMessage(0, generation, 0, appList).sendToTarget();
            }
        }.start();
    }

    protected void getAppList(List<AppInfo> appList) {
        getAppList(appList, this.appTypeIndex);
    }

    protected void getAppList(List<AppInfo> appList, int typeIndex) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appInfoList = pm.getInstalledApplications(0);
        if (typeIndex == 0) {
            for (ApplicationInfo ai : appInfoList) {
                if ((ai.flags & 1) == 0) {
                    appList.add(AppInfo.create(pm, ai));
                }
            }
            return;
        }
        for (ApplicationInfo ai2 : appInfoList) {
            if ((ai2.flags & 1) != 0) {
                appList.add(AppInfo.create(pm, ai2));
            }
        }
    }

    private AppInfo getAppInfo(int position) {
        AppInfo info = null;
        synchronized (this.displayApps) {
            try {
                info = this.displayApps.get(position);
            } catch (Throwable th) {
            }
        }
        return info;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        AppInfo info = getAppInfo(position);
        if (info != null) {
            appClicked(info);
        }
    }

    public void appClicked(AppInfo appInfo) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo moreInfo = pm.getApplicationInfo(appInfo.packagePath, 0);
            String _apkPath = moreInfo.sourceDir;
            if (!MainActivity.upgradedFromOldVersion(this)) {
                RecentFiles.record(this, _apkPath, moreInfo.packageName);
                startFullEditActivity(this, _apkPath);
            } else {
                EditModeDialog dlg = new EditModeDialog(this, this, _apkPath, moreInfo.packageName);
                dlg.show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.menu_more) {
            popListAdapter(v);
        } else if (id == R.id.btn_close) {
            finish();
        } else if (id == R.id.btn_search) {
            searchApp();
        }
    }

    private void searchApp() {
        String keyword = this.keywordEdit.getText().toString();
        String keyword2 = keyword.toLowerCase();
        List<AppInfo> matchedApps = new ArrayList<>();
        for (AppInfo appInfo : this.allApps) {
            if (appInfo.appName.toLowerCase().contains(keyword2)) {
                matchedApps.add(appInfo);
            }
        }
        showAppList(matchedApps);
    }

    private void popListAdapter(View parent) {
        if (this.popupWindow == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService("layout_inflater");
            View view = layoutInflater.inflate(R.layout.popup_list, (ViewGroup) null);
            ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);
            ArrayList<String> groups = new ArrayList<>();
            Resources res = getResources();
            groups.add(res.getString(R.string.user_apps));
            groups.add(res.getString(R.string.system_apps));
            String[] orderLabels = res.getStringArray(R.array.order_key);
            for (String label : orderLabels) {
                groups.add(label);
            }
            this.popListAdapter = new GroupAdapter(this, groups);
            lv_group.setAdapter((ListAdapter) this.popListAdapter);
            int height = Display.dip2px(this, 203.0f);
            int width = Display.getWidth(this) / 2;
            this.popupWindow = new PopupWindow(view, width, height);
            lv_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public final void onItemClick(AdapterView adapterView, View view2, int i, long j) {
                    UserAppActivity.this.m372xb987a84c(adapterView, view2, i, j);
                }
            });
        }
        List<Integer> checkedIndice = new ArrayList<>();
        checkedIndice.add(Integer.valueOf(this.appTypeIndex));
        checkedIndice.add(Integer.valueOf(this.appOrderIndex + 2));
        this.popListAdapter.setCheckIndice(checkedIndice);
        this.popupWindow.setFocusable(true);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService("window");
        int xPos = (windowManager.getDefaultDisplay().getWidth() / 2) - (this.popupWindow.getWidth() / 2);
        this.popupWindow.showAsDropDown(parent, xPos, 0);
    }

    void m372xb987a84c(AdapterView adapterView, View view1, int position, long id) {
        this.appListView.setVisibility(4);
        this.progressBar.setVisibility(0);
        switch (position) {
            case 0:
                reScanAppList(0, this.appOrderIndex);
                break;
            case 1:
                reScanAppList(1, this.appOrderIndex);
                break;
            case 2:
            case 3:
                AppListOrder order = AppListOrder.fromIndex(position - 2);
                SettingActivity.setListOrder(this, order.getPreferenceValue());
                reScanAppList(this.appTypeIndex, order.ordinal());
                break;
        }
        if (this.popupWindow != null) {
            this.popupWindow.dismiss();
        }
    }

    @Override
    public void editModeSelected(int mode, String extraStr) {
        Intent intent = null;
        switch (mode) {
            case 0:
                if (startFullEditActivity(this, extraStr)) {
                    finish();
                    return;
                }
                return;
            case 1:
                intent = new Intent((Context) this, (Class<?>) SimpleEditActivity.class);
                break;
            case 2:
                intent = new Intent((Context) this, (Class<?>) CommonEditActivity.class);
                break;
            case 3:
                try {
                    RefInvoke.invokeStaticMethod("com.gmail.heagoo.apkeditor.pro.appdm", "de", new Class[]{Context.class, String.class}, new Object[]{this, extraStr});
                    break;
                } catch (Exception e) {
                    break;
                }
            case 4:
                intent = new Intent((Context) this, (Class<?>) AxmlEditActivity.class);
                break;
        }
        if (intent != null) {
            ActivityUtil.attachParam(intent, "apkPath", extraStr);
            startActivity(intent);
            finish();
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AppInfo appInfo;
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info == null || (appInfo = getAppInfo(info.position)) == null) {
            return;
        }
        menu.setHeaderTitle(appInfo.appName);
        menu.add(0, 2, 0, R.string.app_info);
        menu.add(0, 3, 0, R.string.backup);
        menu.add(0, 4, 0, R.string.launch);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        switch (item.getItemId()) {
            case 2:
                showAppInfo(position);
                return true;
            case 3:
                backupApp(position);
                return true;
            case 4:
                launchApp(position);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAppInfo(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }
            String packageName = info.packagePath;
            try {
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS"));
            }
        } catch (Exception e2) {
        }
    }

    private void backupApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(info.packagePath, 0);
            final String appName = info.appName;
            final String apkPath = appInfo.publicSourceDir;
            ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
                private String outPath;
                private boolean succeed = false;

                @Override
                public void process() throws Exception {
                    this.outPath = SDCard.makeBackupDir(UserAppActivity.this) + appName + ".apk";
                    FileInputStream in = null;
                    FileOutputStream out = null;
                    try {
                        in = new FileInputStream(apkPath);
                        out = new FileOutputStream(this.outPath);
                        IOUtils.copy(in, out);
                        this.succeed = true;
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                }

                @Override
                public void afterProcess() {
                    if (this.succeed) {
                        String format = UserAppActivity.this.getString(R.string.apk_saved_tip);
                        String message = String.format(format, this.outPath);
                        Toast.makeText((Context) UserAppActivity.this, (CharSequence) message, 1).show();
                    }
                }
            }, -1);
            dlg.show();
        } catch (Exception e) {
        }
    }

    private void launchApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info != null && !openApp(this, info.packagePath)) {
                String message = String.format(getString(R.string.cannot_launch), info.appName);
                Toast.makeText((Context) this, (CharSequence) message, 0).show();
            }
        } catch (Exception e) {
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<UserAppActivity> activityRef;

        public MyHandler(UserAppActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            UserAppActivity activity = this.activityRef.get();
            if (activity == null || activity.isFinishing() || msg.arg1 != activity.scanGeneration) {
                return;
            }
            if (msg.what == 0) {
                @SuppressWarnings("unchecked")
                List<AppInfo> apps = (List<AppInfo>) msg.obj;
                activity.allApps.clear();
                activity.allApps.addAll(apps);
                activity.showAppList(apps);
            }
        }
    }
}
