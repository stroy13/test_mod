package com.gmail.heagoo.pngeditor;

import com.gmail.heagoo.common.ThemedAppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WebViewActivity extends ThemedAppCompatActivity {
    private final int titleId;
    private final String url;
    private final boolean openWithBrowser;
    // If load failed, then go to this URL
    private String failedUrl = null;

    private WebView webView;

    public WebViewActivity(int title, String url, boolean openWithBrowser) {
        this.titleId = title;
        this.url = url;
        this.openWithBrowser = openWithBrowser;
    }

    public WebViewActivity(int title, String url, String failedUrl, boolean openWithBrowser) {
        this.titleId = title;
        this.url = url;
        this.failedUrl = failedUrl;
        this.openWithBrowser = openWithBrowser;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(titleId);


        this.webView = new WebView(this);
        setContentView(this.webView);

        initView();
    }

    private void initView() {
        // If failed, then load another URL
        if (failedUrl != null) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request,
                                            WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    webView.setWebViewClient(new WebViewClient());
                    webView.loadUrl(failedUrl);
                }
            });
        }

        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pngeditor_webview, menu);
        menu.findItem(R.id.action_open_with_browser).setVisible(openWithBrowser);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_close) {
            this.finish();
            return true;
        } else if (id == R.id.action_open_with_browser) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
