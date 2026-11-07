package com.gmail.heagoo.common;

import android.app.Activity;
import android.os.Bundle;

/** Base for framework activities: select the palette before the window is created. */
public class ThemedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EditorTheme.apply(this);
        super.onCreate(savedInstanceState);
    }
}
