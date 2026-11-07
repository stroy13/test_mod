package com.gmail.heagoo.common;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/** Framework preference screens must use the same palette as the rest of the app. */
public class ThemedPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EditorTheme.apply(this);
        super.onCreate(savedInstanceState);
    }
}
