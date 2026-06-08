package com.zixo.call;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fix Google OAuth "disallowed_useragent" error (Error 403)
        // Google blocks sign-in from embedded WebViews by detecting "wv" in the user agent.
        // We remove "wv" and "Version/X.X" so Google treats it as a regular Chrome browser.
        try {
            WebView webView = bridge.getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                String originalUA = settings.getUserAgentString();
                // Remove "wv" (webview identifier) and "Version/X.X" which Google also flags
                String newUA = originalUA
                    .replace("; wv", "")
                    .replaceAll("; Version/\\d+\\.\\d+", "");
                settings.setUserAgentString(newUA);
            }
        } catch (Exception e) {
            // WebView might not be initialized yet — try again after a short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    WebView webView = bridge.getWebView();
                    if (webView != null) {
                        WebSettings settings = webView.getSettings();
                        String originalUA = settings.getUserAgentString();
                        String newUA = originalUA
                            .replace("; wv", "")
                            .replaceAll("; Version/\\d+\\.\\d+", "");
                        settings.setUserAgentString(newUA);
                    }
                } catch (Exception ignored) {}
            }, 500);
        }
    }
}
