package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.common.ThemedPreferenceActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ThemeRegistry;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;

public class SettingActivity extends ThemedPreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final int EXTRACT_AUTORENAME = 0;
    public static final int EXTRACT_OVERWRITE = 1;
    public static final String STR_PRIVATEKEYPATH = "PrivateKeyPath";
    public static final String STR_PUBLICKEYPATH = "PublicKeyPath";
    private static final String apkNameKey = "OutputApkName";
    private static final String appIconKey = "MyIcon";
    private static final String appOrderKey = "AppListOrder";
    private static final String cleanKey = "CleanGarbage";
    private static final String compressLevelKey = "CompressionLevel";
    private static final String confirmEnabledKey = "RebuildConfirmation";
    private static final String decodeDirKey = "DecodeDirectory";
    private static final String decodeModeKey = "DecodeMode";
    private static final String dex2smaliEnabledKey = "SmaliEditingEnabled";
    private static final String fileRenameKey = "FileRenameOption";
    private static final String signWithKey = "SignApkWith";

    private static boolean isFreeVersion() {
        return false;
    }

    public static String getListOrder(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String order = sp.getString(appOrderKey, "");
        String[] orders = ctx.getResources().getStringArray(R.array.order_value);
        for (String str : orders) {
            if (order.equals(str)) {
                return order;
            }
        }
        return orders[0];
    }

    public static int getCompressionLevel(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strLevel = sp.getString(compressLevelKey, "9");
        int level = 9;
        try {
            level = Integer.valueOf(strLevel).intValue();
        } catch (Exception e) {
        }
        if (level < 0) {
            return 0;
        }
        if (level > 9) {
            return 9;
        }
        return level;
    }

    public static String getSignKeyName(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getString(signWithKey, "testkey");
    }

    public static int getOutputApkRule(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strNameRule = sp.getString(apkNameKey, "1");
        try {
            int ret = Integer.valueOf(strNameRule).intValue();
            return ret;
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isDex2smaliEnabled(Context ctx) {
        if (!isFreeVersion()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            return sp.getBoolean(dex2smaliEnabledKey, true);
        }
        return false;
    }

    public static boolean isRebuildConfirmEnabled(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(confirmEnabledKey, false);
    }

    public static int getFileRenameOption(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = sp.getString(fileRenameKey, "1");
        try {
            return Integer.valueOf(str).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    static boolean dirCanWrite(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            String rand = RandomUtil.getRandomString(8);
            File tryF = new File(f, rand);
            boolean ret = tryF.mkdir();
            if (ret) {
                tryF.delete();
            }
            return ret;
        }
        return false;
    }

    public static String getDecodeDirectory(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = sp.getString(decodeDirKey, null);
        if (str != null) {
            if (str.endsWith("/")) {
                str = str.substring(0, str.length() - 1);
            }
            if (dirCanWrite(str)) {
                return str;
            }
        }
        return null;
    }

    public static String getDecodeMode(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String value = sp.getString(decodeModeKey, "2");
        if (!value.equals("0") && !value.equals("1") && !value.equals("2")) {
            return "2";
        }
        return value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(8);
        super.onCreate(savedInstanceState);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        addPreferencesFromResource(R.xml.settings);
        initData();
        findPreference("ToolManager").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                return SettingActivity.this.m291lambda$onCreate$0$comgmailheagooapkeditorSettingActivity(preference);
            }
        });
    }

    boolean m291lambda$onCreate$0$comgmailheagooapkeditorSettingActivity(Preference pref) {
        startActivity(new Intent(this, (Class<?>) ToolManagerActivity.class));
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initData() {
        PreferenceManager manager = getPreferenceManager();
        ListPreference modePref = (ListPreference) manager.findPreference(decodeModeKey);
        String value = getDecodeMode(this);
        modePref.setSummary(getDecodeModeSummary(value));
        modePref.setOnPreferenceChangeListener(this);
        ListPreference orderPref = (ListPreference) manager.findPreference(appOrderKey);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultOrder = getResources().getStringArray(R.array.order_value)[0];
        String order = sp.getString(appOrderKey, defaultOrder);
        orderPref.setValue(order);
        orderPref.setSummary(order);
        orderPref.setOnPreferenceChangeListener(this);
        ListPreference compressLevelPref = (ListPreference) manager.findPreference(compressLevelKey);
        SharedPreferences sp2 = PreferenceManager.getDefaultSharedPreferences(this);
        String strLevel = sp2.getString(compressLevelKey, "9");
        compressLevelPref.setValue(strLevel);
        compressLevelPref.setOnPreferenceChangeListener(this);
        ListPreference signPref = (ListPreference) manager.findPreference(signWithKey);
        SharedPreferences sp3 = PreferenceManager.getDefaultSharedPreferences(this);
        String strKeyname = sp3.getString(signWithKey, "testkey");
        signPref.setValue(strKeyname);
        signPref.setOnPreferenceChangeListener(this);
        ListPreference signPref2 = (ListPreference) manager.findPreference(apkNameKey);
        SharedPreferences sp4 = PreferenceManager.getDefaultSharedPreferences(this);
        String strNameIdx = sp4.getString(apkNameKey, "1");
        signPref2.setValue(strNameIdx);
        signPref2.setOnPreferenceChangeListener(this);
        CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("FullScreen");
        checkBox.setChecked(GlobalConfig.instance(this).isFullScreen());
        checkBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object newValue) {
                GlobalConfig.instance(SettingActivity.this).setFullScreen(SettingActivity.this, ((Boolean) newValue).booleanValue());
                return true;
            }
        });
        ListPreference appThemeList = (ListPreference) findPreference("AppTheme");
        int id = GlobalConfig.instance(this).getThemeId();
        String[] values = getResources().getStringArray(R.array.theme_value);
        appThemeList.setValue(values[id]);
        appThemeList.setSummary(appThemeList.getEntries()[id]);
        appThemeList.setOnPreferenceChangeListener(this);
        ListPreference languageList = (ListPreference) findPreference("Language");
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "");
        languageList.setValue(language);
        languageList.setOnPreferenceChangeListener(this);
        CheckBoxPreference smaliPref = (CheckBoxPreference) findPreference(dex2smaliEnabledKey);
        if (isFreeVersion()) {
            PreferenceCategory category = (PreferenceCategory) findPreference("ApkBuilding");
            category.removePreference(smaliPref);
        } else {
            boolean b = isDex2smaliEnabled(this);
            smaliPref.setChecked(b);
            smaliPref.setSummary(b ? R.string.smali_edit_summary : R.string.smali_edit_disabled_summary);
            smaliPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference arg0, Object newValue) {
                    CheckBoxPreference cb = (CheckBoxPreference) arg0;
                    boolean val = ((Boolean) newValue).booleanValue();
                    cb.setChecked(val);
                    cb.setSummary(val ? R.string.smali_edit_summary : R.string.smali_edit_disabled_summary);
                    return true;
                }
            });
        }
        CheckBoxPreference confirmPref = (CheckBoxPreference) findPreference(confirmEnabledKey);
        boolean enabled = isRebuildConfirmEnabled(this);
        confirmPref.setChecked(enabled);
        confirmPref.setSummary(enabled ? R.string.rebuild_confirm_e_summary : R.string.rebuild_confirm_d_summary);
        confirmPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object newValue) {
                int i;
                CheckBoxPreference cb = (CheckBoxPreference) arg0;
                boolean val = ((Boolean) newValue).booleanValue();
                cb.setChecked(val);
                if (val) {
                    i = R.string.rebuild_confirm_e_summary;
                } else {
                    i = R.string.rebuild_confirm_d_summary;
                }
                cb.setSummary(i);
                return true;
            }
        });
        Preference p = findPreference(cleanKey);
        p.setOnPreferenceClickListener(this);
        ListPreference renamePref = (ListPreference) manager.findPreference(fileRenameKey);
        int option = getFileRenameOption(this);
        renamePref.setValue("" + option);
        EditTextPreference dirPref = (EditTextPreference) manager.findPreference(decodeDirKey);
        dirPref.setOnPreferenceChangeListener(this);
        String decodeDir = getDecodeDirectory(this);
        if (decodeDir != null) {
            dirPref.setSummary(decodeDir);
        }
    }

    private int getDecodeModeSummary(String value) {
        if ("0".equals(value)) {
            return R.string.summary_decode_all_files;
        }
        if ("1".equals(value)) {
            return R.string.summary_decode_partial_files;
        }
        return R.string.summary_decide_at_decoding;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (appOrderKey.equals(key)) {
            String orderValue = (String) newValue;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(appOrderKey, orderValue);
            editor.apply();
            preference.setSummary(orderValue);
        } else if ("AppTheme".equals(key)) {
            try {
                String value = (String) newValue;
                int themeId = Integer.parseInt(value);
                if (!ThemeRegistry.valid(themeId)) {
                    return false;
                }
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("AppTheme", value).apply();
                GlobalConfig.instance(this).updateThemeId(themeId);
                ((ListPreference) preference).setSummary(((ListPreference) preference).getEntries()[themeId]);
                getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        if (SettingActivity.this.isFinishing()) {
                            return;
                        }
                        SettingActivity.this.recreate();
                    }
                });
            } catch (Exception e) {
            }
        } else if (decodeDirKey.equals(key)) {
            String dir = (String) newValue;
            if (dir == null || dir.trim().isEmpty()) {
                preference.setSummary(R.string.decode_dir_summary_may_change);
            } else if (dirCanWrite(dir)) {
                preference.setSummary(dir);
            } else {
                String msg = String.format(getString(R.string.decode_folder_unwritable), dir);
                Toast.makeText(this, msg, 1).show();
                return false;
            }
        } else if (decodeModeKey.equals(key)) {
            preference.setSummary(getDecodeModeSummary((String) newValue));
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String clickedKey = preference.getKey();
        final String path = getFilesDir().getAbsolutePath();
        if (cleanKey.equals(clickedKey)) {
            ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
                @Override
                public void process() throws Exception {
                    SettingActivity.this.cleanSdcard();
                    CommandRunner cr = new CommandRunner();
                    cr.runCommand("rm -rf " + path + "/decoded\nrm -rf " + path + "/temp", (String[]) null, 8000);
                }

                @Override
                public void afterProcess() {
                }
            }, R.string.temp_file_cleaned);
            dlg.show();
            return false;
        }
        return false;
    }

    protected void cleanSdcard() {
        File f = new File(SDCard.getEditorDirectory());
        if (!f.exists() || !f.isDirectory()) {
            return;
        }
        String[] keepingFolders = {"backups", ".projects"};
        File[] subfiles = f.listFiles();
        if (subfiles != null) {
            for (File subfile : subfiles) {
                if (subfile.isDirectory()) {
                    boolean bKeep = false;
                    String folder = subfile.getName();
                    int length = keepingFolders.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String keepDir = keepingFolders[i];
                        if (!folder.equals(keepDir)) {
                            i++;
                        } else {
                            bKeep = true;
                            break;
                        }
                    }
                    if (!bKeep) {
                        cleanDirectory(subfile);
                        subfile.delete();
                    }
                } else {
                    subfile.delete();
                }
            }
        }
    }

    protected void cleanDirectory(File dir) {
        File[] subfiles = dir.listFiles();
        if (subfiles != null) {
            for (File f : subfiles) {
                if (f.isDirectory()) {
                    cleanDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
        }
    }
}
