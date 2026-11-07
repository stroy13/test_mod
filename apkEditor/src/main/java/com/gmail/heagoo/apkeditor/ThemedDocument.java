package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.util.TypedValue;
import android.webkit.WebView;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/* loaded from: /tmp/apkx/classes5.dex */
final class ThemedDocument {
    private ThemedDocument() {
    }

    private static int color(Context c, int attr, int fallback) {
        TypedValue value = new TypedValue();
        return !c.getTheme().resolveAttribute(attr, value, true) ? fallback : value.resourceId != 0 ? c.getResources().getColor(value.resourceId) : value.data;
    }

    private static String cssColor(int color) {
        return String.format(Locale.US, "#%06X", Integer.valueOf(16777215 & color));
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x0073  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x00c8 A[Catch: Exception -> 0x010a, TryCatch #1 {Exception -> 0x010a, blocks: (B:3:0x0030, B:11:0x004f, B:13:0x005a, B:15:0x0060, B:18:0x006e, B:21:0x0078, B:23:0x00c8, B:24:0x00ef, B:27:0x00e2, B:29:0x0064, B:31:0x0068, B:36:0x0106, B:37:0x0109, B:6:0x0041, B:7:0x0043, B:9:0x004b), top: B:2:0x0030, inners: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00e2 A[Catch: Exception -> 0x010a, TryCatch #1 {Exception -> 0x010a, blocks: (B:3:0x0030, B:11:0x004f, B:13:0x005a, B:15:0x0060, B:18:0x006e, B:21:0x0078, B:23:0x00c8, B:24:0x00ef, B:27:0x00e2, B:29:0x0064, B:31:0x0068, B:36:0x0106, B:37:0x0109, B:6:0x0041, B:7:0x0043, B:9:0x004b), top: B:2:0x0030, inners: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0076  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static void load(WebView view, int resource) {
        boolean z;
        Context c = view.getContext();
        int bg = color(c, R.attr.labSurfaceAlt, -1);
        String background = cssColor(bg);
        String text = cssColor(color(c, R.attr.labText, -15195082));
        String accent = cssColor(color(c, R.attr.labAccent, -13214523));
        view.setBackgroundColor(bg);
        try {
            InputStream in = c.getResources().openRawResource(resource);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try {
                byte[] chunk = new byte[4096];
                while (true) {
                    int count = in.read(chunk);
                    if (count == -1) {
                        break;
                    } else {
                        bytes.write(chunk, 0, count);
                    }
                }
                in.close();
                byte[] data = bytes.toByteArray();
                if (data.length >= 2) {
                    z = true;
                    if ((data[0] == -1 && data[1] == -2) || (data[0] == -2 && data[1] == -1)) {
                        boolean utf16 = z;
                        String html = new String(data, !utf16 ? "UTF-16" : "UTF-8").replaceAll("(?i)charset\\s*=\\s*(utf-16|us-ascii|windows-1252)", "charset=utf-8");
                        String css = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style type=\"text/css\">html,body{background-color:" + background + " !important;}body{margin:0;padding:16px;line-height:1.55;overflow-wrap:break-word;word-wrap:break-word;}body *{background-color:transparent !important;color:" + text + " !important;font-family:sans-serif !important;font-size:14px !important;}body,body p,body li{color:" + text + " !important;font-family:sans-serif !important;font-size:14px !important;line-height:1.55 !important;}body a,body a *{color:" + accent + " !important;}body h1,body h1 *,body h2,body h2 *{font-size:19px !important;}body h3,body h3 *{font-size:16px !important;}body pre,body code,body pre *,body code *{font-family:monospace !important;white-space:pre-wrap !important;}img{max-width:100%;height:auto;}table{max-width:100%;}</style>";
                        int headEnd = html.toLowerCase(Locale.US).indexOf("</head>");
                        view.loadDataWithBaseURL("file:///android_res/raw/", (headEnd < 0 ? new StringBuilder().append(html.substring(0, headEnd)).append(css).append(html.substring(headEnd)) : new StringBuilder().append(css).append(html)).toString(), "text/html", "UTF-8", null);
                    }
                }
                z = false;
                boolean utf162 = z;
                String html2 = new String(data, !utf162 ? "UTF-16" : "UTF-8").replaceAll("(?i)charset\\s*=\\s*(utf-16|us-ascii|windows-1252)", "charset=utf-8");
                String css2 = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style type=\"text/css\">html,body{background-color:" + background + " !important;}body{margin:0;padding:16px;line-height:1.55;overflow-wrap:break-word;word-wrap:break-word;}body *{background-color:transparent !important;color:" + text + " !important;font-family:sans-serif !important;font-size:14px !important;}body,body p,body li{color:" + text + " !important;font-family:sans-serif !important;font-size:14px !important;line-height:1.55 !important;}body a,body a *{color:" + accent + " !important;}body h1,body h1 *,body h2,body h2 *{font-size:19px !important;}body h3,body h3 *{font-size:16px !important;}body pre,body code,body pre *,body code *{font-family:monospace !important;white-space:pre-wrap !important;}img{max-width:100%;height:auto;}table{max-width:100%;}</style>";
                int headEnd2 = html2.toLowerCase(Locale.US).indexOf("</head>");
                view.loadDataWithBaseURL("file:///android_res/raw/", (headEnd2 < 0 ? new StringBuilder().append(html2.substring(0, headEnd2)).append(css2).append(html2.substring(headEnd2)) : new StringBuilder().append(css2).append(html2)).toString(), "text/html", "UTF-8", null);
            } catch (Throwable th) {
                in.close();
                throw th;
            }
        } catch (Exception e) {
            view.loadUrl("file:///android_res/raw/" + c.getResources().getResourceEntryName(resource) + ".htm");
        }
    }
}
