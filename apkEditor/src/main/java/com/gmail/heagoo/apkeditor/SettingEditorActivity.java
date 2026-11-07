package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.EditorTheme;
import com.gmail.heagoo.neweditor.ParserRule;

public class SettingEditorActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    public static boolean isLineWrap(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("LineWrap", true);
    }

    public static int getFontSize(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return getFontSize(sp);
    }

    public static boolean symbolInputEnabled(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("SymbolInput", true);
    }

    private static int getFontSize(SharedPreferences sp) {
        String strFontSize = sp.getString("FontSize", "12");
        try {
            int fontSize = Integer.valueOf(strFontSize).intValue();
            return fontSize;
        } catch (Exception e) {
            return 12;
        }
    }

    private static int getBigFileThreshold(SharedPreferences sp) {
        String strThreshold = sp.getString("BigFileSize", "64");
        try {
            int threshold = Integer.valueOf(strThreshold).intValue();
            return threshold;
        } catch (Exception e) {
            return 64;
        }
    }

    public static int getBigFileThreshold(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return getBigFileThreshold(sp);
    }

    public static boolean showLineNumbers(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("ShowLineNumbers", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EditorTheme.apply(this);
        super.onCreate(savedInstanceState);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        addPreferencesFromResource(R.xml.editor_setting);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        CheckBoxPreference checkbox = (CheckBoxPreference) findPreference("LineWrap");
        checkbox.setOnPreferenceChangeListener(this);
        boolean enabled = sp.getBoolean("LineWrap", true);
        if (enabled) {
            checkbox.setSummary(R.string.line_wrap_enabled);
            checkbox.setChecked(true);
        } else {
            checkbox.setSummary(R.string.line_wrap_disabled);
            checkbox.setChecked(false);
        }
        EditTextPreference pref = (EditTextPreference) findPreference("FontSize");
        pref.setOnPreferenceChangeListener(this);
        int fontSize = getFontSize(sp);
        pref.setSummary(String.format(getString(R.string.font_size_summary), Integer.valueOf(fontSize)));
        EditTextPreference pref2 = (EditTextPreference) findPreference("BigFileSize");
        pref2.setOnPreferenceChangeListener(this);
        int filesize = getBigFileThreshold(sp);
        pref2.setSummary(String.format(getString(R.string.use_bfe_summary), Integer.valueOf(filesize)));
        CheckBoxPreference checkbox2 = (CheckBoxPreference) findPreference("ShowLineNumbers");
        checkbox2.setOnPreferenceChangeListener(this);
        boolean enabled2 = sp.getBoolean("ShowLineNumbers", true);
        if (enabled2) {
            checkbox2.setSummary(R.string.line_numebrs_enabled);
            checkbox2.setChecked(true);
        } else {
            checkbox2.setSummary(R.string.line_numbers_disabled);
            checkbox2.setChecked(false);
        }
        CheckBoxPreference checkbox3 = (CheckBoxPreference) findPreference("SymbolInput");
        boolean enabled3 = sp.getBoolean("SymbolInput", true);
        checkbox3.setChecked(enabled3);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int fileSize;
        int fontSize;
        String key = preference.getKey();
        if ("LineWrap".equals(key)) {
            Boolean b = (Boolean) newValue;
            if (b.booleanValue()) {
                preference.setSummary(R.string.line_wrap_enabled);
            } else {
                preference.setSummary(R.string.line_wrap_disabled);
            }
        } else if ("FontSize".equals(key)) {
            String strFontSize = (String) newValue;
            try {
                fontSize = Integer.valueOf(strFontSize).intValue();
            } catch (Exception e) {
                fontSize = getFontSize(this);
            }
            preference.setSummary(String.format(getString(R.string.font_size_summary), Integer.valueOf(fontSize)));
        } else if ("BigFileSize".equals(key)) {
            String strSize = (String) newValue;
            try {
                fileSize = Integer.valueOf(strSize).intValue();
            } catch (Exception e2) {
                fileSize = getBigFileThreshold(this);
            }
            preference.setSummary(String.format(getString(R.string.use_bfe_summary), Integer.valueOf(fileSize)));
        }
        if ("ShowLineNumbers".equals(key)) {
            Boolean b2 = (Boolean) newValue;
            if (b2.booleanValue()) {
                preference.setSummary(R.string.line_numebrs_enabled);
                return true;
            }
            preference.setSummary(R.string.line_numbers_disabled);
            return true;
        }
        return true;
    }
}
