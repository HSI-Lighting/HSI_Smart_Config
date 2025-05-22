package uk.co.hsilighting.smart_config;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure);
        WebView webView = findViewById(R.id.config_webview);

        // Enable JavaScript (optional)
        webView.getSettings().setJavaScriptEnabled(true);
        // For Android 8.0 (API 26) and below

// Allow all content
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Get the URL passed from the previous activity
        String IP = getIntent().getStringExtra("IP");

        // Load the URL if it exists
        if (IP != null && !IP.isEmpty()) {
            webView.loadUrl("http://" + IP);
        } else {
            // Handle case where no URL was provided
            webView.loadUrl("https://www.google.com"); // default URL
        }

        // Enable back navigation within WebView history
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
    }
}