package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import app.crossword.yourealwaysbe.forkyz.R;

public class WebBrowserActivity extends ForkyzActivity {
    private EditText mURL;
    private InputMethodManager mIMM;
    private PuzzleDownloadListener mPDL;
    private WebView mWebView;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_browser_view);

        (findViewById(R.id.close)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // This tells the download picker to close itself.
                    WebBrowserActivity.this.setResult(RESULT_OK);
                    WebBrowserActivity.this.finish();
                }
            });

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setInitialScale(1);

        final WebSettings webSettings = mWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);

        mPDL = new PuzzleDownloadListener(this, getFileHandler());

        mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request
                ) {
                    String url = request.getUrl().toString();
                    if (url.endsWith(".puz")) {
                        // Misconfigured server not reporting download - start it anyway.
                        mPDL.onDownloadStart(url, webSettings.getUserAgentString(), null, null, 0);

                        return true;
                    }

                    view.loadUrl(url);
                    mURL.setText(url);

                    return true;
                }
            });

        mWebView.setDownloadListener(mPDL);

        mIMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mURL = (EditText) findViewById(R.id.url);
        mURL.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        String url = mURL.getText()
                                         .toString();

                        if (!url.contains("://")) {
                            mURL.setText("http://" + url);
                        }

                        mWebView.loadUrl(mURL.getText().toString());
                        mIMM.hideSoftInputFromWindow(mURL.getWindowToken(), 0);

                        return true;
                    }

                    return false;
                }
            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
