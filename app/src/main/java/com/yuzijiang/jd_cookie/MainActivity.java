package com.yuzijiang.jd_cookie;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "作者QQ：2741744509", Toast.LENGTH_LONG).show();

        WebView webview = findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true); // 启用javascript
        settings.setDomStorageEnabled(true); // 支持HTML5中的一些控件标签
        settings.setBuiltInZoomControls(false); // 自选，非必要

        //处理http和https混合的问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else {
            settings.setMixedContentMode(WebSettings.LOAD_NORMAL);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 允许javascript出错
            try {
                Method method = Class.forName("android.webkit.WebView").
                        getMethod("setWebContentsDebuggingEnabled", Boolean.TYPE);
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(null, true);
                }
            } catch (Exception e) {
                // do nothing
            }
        }


        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) { // 显示加载进度，自选
                //注意textView的视图层级应该在webView上，不然就被webView遮挡住了
                TextView progressTV = findViewById(R.id.progressTV);
                progressTV.setText(String.format(Locale.CHINA, "%d%%", progress));
                progressTV.setVisibility((progress > 0 && progress < 100) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                callback.invoke(origin, true, false); // 页面有请求位置的时候需要
            }
        });


        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) { // 4.0以上必须要加
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //super.onReceivedSslError(view, handler, error)
                switch (error.getPrimaryError()) {
                    case SslError.SSL_INVALID: // 校验过程遇到了bug
                    case SslError.SSL_UNTRUSTED: // 证书有问题
                        handler.proceed();
                    default:
                        handler.cancel();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(url);


                Pattern pattern1 = Pattern.compile("(pt_key=)(.*?)(;)");
                Matcher matcher1 = pattern1.matcher(cookie);
                String str = "";
                if (matcher1.find()) {
                    str = "pt_key=" + matcher1.group(2) + ";";
                }

                Pattern pattern2 = Pattern.compile("(pt_pin=)(.*?)(;)");
                Matcher matcher2 = pattern2.matcher(cookie);
                if (matcher2.find()) {
                    str += "pt_pin=" + matcher2.group(2) + ";";
                }

                String finalStr = str;
                if (str != ""){
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("复制京东Cookie")
                            .setMessage(str)
                            .setPositiveButton("复制", (dialog, which) -> {
                                copyStr(finalStr);
                                Toast.makeText(MainActivity.this, "复制成功！", Toast.LENGTH_SHORT).show();

                                //清空cookie
                                CookieSyncManager.createInstance(MainActivity.this);
                                cookieManager.removeAllCookie();

                                webview.loadUrl("https://home.m.jd.com/myJd/home.action");
                            })
                            .create()
                            .show();
                }

                super.onPageFinished(view, url);
            }

            /**
             * 复制内容到剪贴板
             */
            private boolean copyStr(String copyStr) {
                try {
                    //获取剪贴板管理器
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    // 创建普通字符型ClipData
                    ClipData mClipData = ClipData.newPlainText("Label", copyStr);
                    // 将ClipData内容放到系统剪贴板里。
                    cm.setPrimaryClip(mClipData);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });


        webview.loadUrl("https://home.m.jd.com/myJd/home.action");

    }
}