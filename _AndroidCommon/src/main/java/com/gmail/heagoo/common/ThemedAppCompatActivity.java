package com.gmail.heagoo.common;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Base for AppCompat activities, including the PNG editor and help screens. */
public class ThemedAppCompatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EditorTheme.apply(this);
        super.onCreate(savedInstanceState);
    }
}
