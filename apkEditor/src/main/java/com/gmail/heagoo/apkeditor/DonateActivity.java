package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.common.ThemedActivity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.neweditor.Encoding;

public class DonateActivity extends ThemedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        initIcon();
        initWeb();
    }

    private void initIcon() {
        ImageView iv = (ImageView) findViewById(R.id.image_launcher);
        ApplicationInfo ai = getApplicationInfo();
        iv.setImageDrawable(ai.loadIcon(getPackageManager()));
    }

    private void initWeb() {
        WebView v = (WebView) findViewById(R.id.donateWeb);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < "ifbhpp".length(); i++) {
            char decode = (char) ("ifbhpp".charAt(i) - 1);
            sb.append(decode);
        }
        String account = sb.toString();
        String content = "<html>\n<body>\n\n<center><h1>Support the author</h1></center>\n<p>\nCould you please support a small app developer by paying a little money?<br>\n</p>\n<p>\nWith your money support, the author will get motivated to continuously improve APK Editor (Pro).<br>\n</p>\n<p>\nThanks for your help!<br>\n</p>\n\n\n<center>\n<a href=\"https://www.paypal.me/" + account + "/1.99\"><button style=\"width:80%\">Basic Support: Donate $1.99</button></a><br><br>\n<a href=\"https://www.paypal.me/" + account + "/4.99\"><button style=\"width:80%\">Silver Support: Donate $4.99</button></a><br><br>\n<a href=\"https://www.paypal.me/" + account + "/9.99\"><button style=\"width:80%\">Gold Support: Donate $9.99</button></a><br><br>\n<a href=\"https://www.paypal.me/" + account + "/\"><button style=\"width:80%\">I'd like to input the money myself</button></a><br><br>\n</center>\n\n</body>\n</html>";
        v.loadData(content, "text/html", Encoding.DEFAULT_CHARSET_NAME);
    }
}
