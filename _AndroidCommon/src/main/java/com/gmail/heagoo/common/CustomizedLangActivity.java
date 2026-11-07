package com.gmail.heagoo.common;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.util.Locale;

/** Shared language and theme setup for the editor's framework activities. */
public class CustomizedLangActivity extends ThemedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "");
        if (!language.isEmpty()) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            // Copy the current configuration: creating an empty one resets night mode,
            // density, font scale and other device settings while changing language.
            Configuration config = new Configuration(getResources().getConfiguration());
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }
}
