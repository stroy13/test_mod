package com.gmail.heagoo.apkeditor;

import android.os.Bundle;
import android.webkit.WebView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.neweditor.ParserRule;

public class HelpActivity extends CustomizedLangActivity {
    /* JADX WARN: Multi-variable type inference failed */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setContentView(R.layout.activity_help);
        WebView v = (WebView) findViewById(R.id.helpWeb);
        ThemedDocument.load(v, R.raw.help);
    }
}
