package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.webkit.WebView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ThemeColors;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/** Loads bundled HTML help/about documents using the selected palette. */
final class ThemedDocument {
    private ThemedDocument() {
    }

    private static String cssColor(int color) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
    }

    static void load(WebView view, int resource) {
        Context context = view.getContext();
        int background = ThemeColors.color(context, R.attr.labSurfaceAlt);
        int text = ThemeColors.color(context, R.attr.labText);
        int accent = ThemeColors.color(context, R.attr.labAccent);
        view.setBackgroundColor(background);
        try (InputStream in = context.getResources().openRawResource(resource)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int count;
            while ((count = in.read(chunk)) != -1) {
                bytes.write(chunk, 0, count);
            }
            byte[] data = bytes.toByteArray();
            String charset = "UTF-8";
            int offset = 0;
            if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
                charset = "UTF-16BE";
                offset = 2;
            } else if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
                charset = "UTF-16LE";
                offset = 2;
            }
            String html = new String(data, offset, data.length - offset, Charset.forName(charset))
                    .replaceAll("(?i)charset\\s*=\\s*(utf-16|us-ascii|windows-1252)", "charset=utf-8");
            String css = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                    + "<style type=\"text/css\">"
                    + "html,body{background-color:" + cssColor(background) + " !important;}"
                    + "body{margin:0;padding:16px;line-height:1.55;overflow-wrap:break-word;word-wrap:break-word;}"
                    + "body *{background-color:transparent !important;color:" + cssColor(text) + " !important;font-family:sans-serif !important;font-size:14px !important;}"
                    + "body,body p,body li{color:" + cssColor(text) + " !important;line-height:1.55 !important;}"
                    + "body a,body a *{color:" + cssColor(accent) + " !important;}"
                    + "body h1,body h1 *,body h2,body h2 *{font-size:19px !important;}"
                    + "body h3,body h3 *{font-size:16px !important;}"
                    + "body pre,body code,body pre *,body code *{font-family:monospace !important;white-space:pre-wrap !important;}"
                    + "img{max-width:100%;height:auto;}table{max-width:100%;}</style>";
            int headEnd = html.toLowerCase(Locale.US).indexOf("</head>");
            String themed = headEnd < 0
                    ? css + html
                    : html.substring(0, headEnd) + css + html.substring(headEnd);
            view.loadDataWithBaseURL("file:///android_res/raw/", themed, "text/html", "UTF-8", null);
        } catch (Exception e) {
            view.loadUrl("file:///android_res/raw/" + context.getResources().getResourceEntryName(resource) + ".htm");
        }
    }
}
