package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.common.ThemedAppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.gmail.heagoo.apkeditor.EditModeDialog;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.RecentHistory;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.prj.ProjectListActivity;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.apkeditor.util.OnlineMessage;
import com.gmail.heagoo.apkeditor.util.ToolUpdateChecker;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.httpserver.HttpServiceManager;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ThemedAppCompatActivity implements AdapterView.OnItemClickListener, ProcessingDialog.ProcessingInterface {
    private static int g_vcRet;
    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private OnlineMessage prompter;

    public static native int isX86();

    public static native void it(Object obj, String str, String str2, String str3);

    public static native void md(String str, String str2, String str3, int i, String str4, int i2, String str5, int i3);

    public static native void mg(String str, String str2, String str3, int i, String str4, int i2);

    public static native int vc(Object obj, int i);

    static {
        System.loadLibrary("syscheck");
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown") || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86") || Build.MANUFACTURER.contains("Genymotion") || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) || "google_sdk".equals(Build.PRODUCT);
    }

    public static boolean upgradedFromOldVersion(Context ctx) {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ToolUpdateChecker.check(this, false, (Runnable) null);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setupLanguage();
        setContentView(R.layout.activity_main);
        initUI();
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo moreInfo = pm.getApplicationInfo(getPackageName(), 0);
            String apkPath = moreInfo.sourceDir;
            it(getApplicationContext(), getPackageName(), getFilesDir().getPath(), apkPath);
        } catch (Exception e) {
        }
        initFileWithPermissionCheck();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void setupLanguage() {
        String languageToLoad = PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "");
        if (!languageToLoad.equals("")) {
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            int i = Build.VERSION.SDK_INT;
            config.setLocale(locale);
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onResume() {
        super.onResume();
        if (StorageAccess.has(this)) {
            initFile();
        }
        renderRecentFiles();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void checkVersion() {
        String pkgName = getPackageName();
        if (pkgName.charAt(pkgName.length() - 1) == 'o') {
            String installer = getPackageManager().getInstallerPackageName(pkgName);
            if (installer == null || !installer.endsWith(".vending")) {
                Toast.makeText((Context) this, (CharSequence) "Please Install it from Google Play!", 1).show();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void setupSlidingMenu() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        this.mDrawerLayout = findViewById(R.id.drawer_layout);
        this.mDrawerList = (ListView) findViewById(R.id.slider_list);
        MenuListAdapter adapter = new MenuListAdapter(this, GlobalConfig.instance(this).isDarkTheme());
        this.mDrawerList.setAdapter((ListAdapter) adapter);
        this.mDrawerList.setOnItemClickListener(this);
        findViewById(R.id.home_menu_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m229x793d3270(view);
            }
        });
        findViewById(R.id.home_settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m230x5dd5d71(view);
            }
        });
        findViewById(R.id.home_info_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m231x927d8872(view);
            }
        });
    }

    void m229x793d3270(View v) {
        this.mDrawerLayout.openDrawer(8388611);
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m230x5dd5d71(View v) {
        startActivity(new Intent((Context) this, (Class<?>) SettingActivity.class));
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m231x927d8872(View v) {
        new AboutDialog(this).show();
    }

    private void initUI() {
        setupSlidingMenu();
        TextView brand = (TextView) findViewById(R.id.home_brand_title);
        SpannableString title = new SpannableString(getString(R.string.about_apk_editor_title));
        TypedValue accent = new TypedValue();
        getTheme().resolveAttribute(R.attr.labAccent, accent, true);
        int plus = title.toString().lastIndexOf(43);
        if (plus >= 0) {
            title.setSpan(new ForegroundColorSpan(accent.data), plus, plus + 1, 33);
        }
        brand.setText(title);
        final TextView openApkBtn = (TextView) findViewById(R.id.tv_select_apkfile);
        openApkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m221lambda$initUI$3$comgmailheagooapkeditorMainActivity(view);
            }
        });
        final TextView openAppBtn = (TextView) findViewById(R.id.tv_select_appfile);
        openAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m222lambda$initUI$4$comgmailheagooapkeditorMainActivity(view);
            }
        });
        findViewById(R.id.home_apk_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                openApkBtn.performClick();
            }
        });
        findViewById(R.id.home_apps_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                openAppBtn.performClick();
            }
        });
        findViewById(R.id.home_odex_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m223lambda$initUI$8$comgmailheagooapkeditorMainActivity(view);
            }
        });
        final TextView exitButton = (TextView) findViewById(R.id.tv_exit);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m224lambda$initUI$9$comgmailheagooapkeditorMainActivity(view);
            }
        });
        final TextView helpButton = (TextView) findViewById(R.id.tv_help);
        findViewById(R.id.home_help_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                helpButton.performClick();
            }
        });
        findViewById(R.id.home_exit_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                exitButton.performClick();
            }
        });
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m220lambda$initUI$13$comgmailheagooapkeditorMainActivity(view);
            }
        });
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m221lambda$initUI$3$comgmailheagooapkeditorMainActivity(View v) {
        Intent intent = new Intent((Context) this, (Class<?>) FileListActivity.class);
        startActivity(intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m222lambda$initUI$4$comgmailheagooapkeditorMainActivity(View v) {
        Intent intent = new Intent((Context) this, (Class<?>) UserAppActivity.class);
        startActivity(intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void lambda$initUI$5(View v) {
        Intent intent = new Intent((Context) this, (Class<?>) SettingActivity.class);
        startActivity(intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m223lambda$initUI$8$comgmailheagooapkeditorMainActivity(View v) {
        startActivity(new Intent((Context) this, (Class<?>) OdexPatchActivity.class));
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m224lambda$initUI$9$comgmailheagooapkeditorMainActivity(View v) {
        if (System.currentTimeMillis() < 3600000) {
            test();
        }
        new ProcessingDialog(this, this, -1).show();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void lambda$initUI$12(View v) {
        Intent helpIntent = new Intent((Context) this, (Class<?>) ProjectListActivity.class);
        startActivity(helpIntent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m220lambda$initUI$13$comgmailheagooapkeditorMainActivity(View v) {
        Intent helpIntent = new Intent((Context) this, (Class<?>) HelpActivity.class);
        startActivity(helpIntent);
    }

    private int homeDp(int dp) {
        return (int) ((dp * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int homeColor(int attr) {
        TypedValue v = new TypedValue();
        getTheme().resolveAttribute(attr, v, true);
        return v.data;
    }

    private int homeDrawable(int attr) {
        TypedValue v = new TypedValue();
        getTheme().resolveAttribute(attr, v, true);
        return v.resourceId;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void renderRecentFiles() {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.home_recent_container);
        View empty = findViewById(R.id.home_recent_empty_panel);
        if (linearLayout != null && empty != null) {
            List<RecentHistory.Entry> entries = RecentFiles.list(this);
            linearLayout.removeAllViews();
            int i = 0;
            empty.setVisibility(entries.isEmpty() ? 0 : 8);
            for (final RecentHistory.Entry entry : entries) {
                String label = new File(entry.path).getName();
                if (!entry.packageName.isEmpty()) {
                    try {
                        ApplicationInfo info = getPackageManager().getApplicationInfo(entry.packageName, i);
                        label = getPackageManager().getApplicationLabel(info).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                if (label.isEmpty()) {
                    label = entry.path;
                }
                LinearLayout linearLayout2 = new LinearLayout(this);
                linearLayout2.setOrientation(i);
                linearLayout2.setGravity(16);
                linearLayout2.setPadding(homeDp(12), homeDp(10), homeDp(12), homeDp(10));
                linearLayout2.setMinimumHeight(homeDp(64));
                linearLayout2.setBackgroundResource(homeDrawable(R.attr.homeActionBackground));
                linearLayout2.setClickable(true);
                linearLayout2.setFocusable(true);
                ImageView icon = new ImageView(this);
                icon.setImageResource(R.drawable.home_history_icon);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(homeDp(28), homeDp(28));
                linearLayout2.addView(icon, ip);
                LinearLayout words = new LinearLayout(this);
                words.setOrientation(1);
                LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(i, -2, 1.0f);
                wp.leftMargin = homeDp(12);
                wp.rightMargin = homeDp(8);
                linearLayout2.addView(words, wp);
                TextView name = new TextView(this);
                name.setText(label);
                name.setTextColor(homeColor(R.attr.labText));
                name.setTextSize(14.0f);
                name.setTypeface(null, 1);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                words.addView(name);
                TextView path = new TextView(this);
                path.setText(entry.packageName.isEmpty() ? entry.path : entry.packageName);
                path.setTextSize(11.0f);
                path.setTextColor(homeColor(R.attr.labMuted));
                path.setSingleLine(true);
                path.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                words.addView(path);
                ImageView arrow = new ImageView(this);
                arrow.setImageResource(R.drawable.home_arrow);
                View empty2 = empty;
                List<RecentHistory.Entry> entries2 = entries;
                linearLayout2.addView(arrow, new LinearLayout.LayoutParams(homeDp(20), homeDp(20)));
                linearLayout2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        MainActivity.this.m226x7551cdd5(entry, view);
                    }
                });
                linearLayout2.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public final boolean onLongClick(View view) {
                        return MainActivity.this.m228x8e9223d7(entry, view);
                    }
                });
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                if (linearLayout.getChildCount() > 0) {
                    rp.topMargin = homeDp(8);
                }
                linearLayout.addView(linearLayout2, rp);
                empty = empty2;
                entries = entries2;
                i = 0;
            }
        }
    }

    void m226x7551cdd5(RecentHistory.Entry entry, View v) {
        openRecent(entry);
    }

    /* JADX WARN: Multi-variable type inference failed */
    boolean m228x8e9223d7(final RecentHistory.Entry entry, View v) {
        new AlertDialog.Builder(this).setMessage(R.string.home_recent_remove).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.home_recent_remove_action, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m227x1f1f8d6(entry, dialogInterface, i);
            }
        }).show();
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m227x1f1f8d6(RecentHistory.Entry entry, DialogInterface d, int w) {
        RecentFiles.remove(this, entry);
        renderRecentFiles();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void openRecent(RecentHistory.Entry entry) {
        String path = entry.path;
        if (!entry.packageName.isEmpty()) {
            try {
                path = getPackageManager().getApplicationInfo(entry.packageName, 0).sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                Toast.makeText((Context) this, R.string.home_recent_missing, 1).show();
                return;
            }
        }
        if (!new File(path).isFile() || !new File(path).canRead()) {
            Toast.makeText((Context) this, R.string.home_recent_missing, 1).show();
        } else if (upgradedFromOldVersion(this)) {
            new EditModeDialog(this, new EditModeDialog.IEditModeSelected() {
                @Override
                public final void editModeSelected(int i, String str) {
                    MainActivity.this.m225lambda$openRecent$17$comgmailheagooapkeditorMainActivity(i, str);
                }
            }, path, entry.packageName.isEmpty() ? null : entry.packageName).show();
        } else {
            RecentFiles.record(this, path, entry.packageName);
            UserAppActivity.startFullEditActivity(this, path);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    void m225lambda$openRecent$17$comgmailheagooapkeditorMainActivity(int mode, String extra) {
        Intent intent;
        if (mode == 0) {
            UserAppActivity.startFullEditActivity(this, extra);
            return;
        }
        if (mode == 3) {
            try {
                RefInvoke.invokeStaticMethod("com.gmail.heagoo.apkeditor.pro.appdm", "de", new Class[]{Context.class, String.class}, new Object[]{this, extra});
                return;
            } catch (Exception e) {
                Toast.makeText((Context) this, (CharSequence) e.toString(), 1).show();
                return;
            }
        }
        if (mode == 1) {
            intent = new Intent((Context) this, (Class<?>) SimpleEditActivity.class);
        } else if (mode == 2) {
            intent = new Intent((Context) this, (Class<?>) CommonEditActivity.class);
        } else if (mode != 4) {
            return;
        } else {
            intent = new Intent((Context) this, (Class<?>) AxmlEditActivity.class);
        }
        ActivityUtil.attachParam(intent, "apkPath", extra);
        startActivity(intent);
    }

    private void test() {
        g_vcRet = vc(getApplicationContext(), 0);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mDrawerToggle != null && this.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent((Context) this, (Class<?>) SettingActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            new AboutDialog(this).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = (this.mDrawerLayout == null || this.mDrawerList == null || !this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) ? false : true;
        if (menu.findItem(R.id.action_settings) != null) {
            menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        }
        if (menu.findItem(R.id.action_about) != null) {
            menu.findItem(R.id.action_about).setVisible(drawerOpen ? false : true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.mDrawerToggle != null) {
            this.mDrawerToggle.syncState();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mDrawerToggle != null) {
            this.mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    public void onBackPressed() {
        if (this.mDrawerLayout == null || this.mDrawerList == null || !this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) {
            finish();
        } else {
            this.mDrawerLayout.closeDrawer(this.mDrawerList);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == 0) {
            initFile();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void initFileWithPermissionCheck() {
        if (!StorageAccess.has(this)) {
            StorageAccess.request(this, this.WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            initFile();
        }
    }

    private void initFile() {
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        switch ((int) id) {
            case 0:
                Intent intent = new Intent((Context) this, (Class<?>) ProjectListActivity.class);
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent((Context) this, (Class<?>) DonateActivity.class);
                startActivity(intent2);
                break;
            case 2:
                Intent intent3 = new Intent((Context) this, (Class<?>) SettingActivity.class);
                startActivity(intent3);
                break;
            case 3:
                new AboutDialog(this).show();
                break;
            case 4:
                startActivity(new Intent((Context) this, (Class<?>) ApkInspectionActivity.class));
                break;
        }
        this.mDrawerLayout.closeDrawer(this.mDrawerList);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override
    public void process() throws Exception {
        try {
            Intent intent = new Intent((Context) this, (Class<?>) ApkComposeService.class);
            stopService(intent);
            HttpServiceManager.instance().stopWebService(this);
            File fileDir = getFilesDir();
            String rootDirectory = fileDir.getAbsolutePath();
            String decodeRootPath = rootDirectory + "/decoded";
            FileUtil.deleteAll(new File(decodeRootPath));
        } catch (Throwable th) {
        }
    }

    @Override
    public void afterProcess() {
        Process.killProcess(Process.myPid());
    }
}
