package io.github.park03059.fitphoto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String HOME = "https://park03059-netizen.github.io/parkminseok/";
    private static final String ORIGIN = "https://park03059-netizen.github.io";
    private static final int PICK_FILE = 1, SAVE_FILE = 2;
    private static final int MAX_BASE64 = 32 * 1024 * 1024;
    private WebView web;
    private ProgressBar progress;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingDownload;
    private boolean receivingDownload;
    private String downloadScript;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorPanel.setBackgroundColor(0xfff4f7fa);
        TextView message = new TextView(this);
        message.setText("사이트에 연결할 수 없습니다.\n인터넷 연결을 확인한 뒤 다시 시도해 주세요.");
        message.setGravity(Gravity.CENTER);
        message.setTextSize(17);
        errorPanel.addView(message);
        Button retry = new Button(this);
        retry.setText("다시 시도");
        retry.setOnClickListener(v -> web.loadUrl(HOME));
        errorPanel.addView(retry);
        root.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        errorPanel.setVisibility(View.GONE);
        setContentView(root);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);

        try (var in = getAssets().open("downloads.js")) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int count;
            while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            downloadScript = buffer.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) { throw new IllegalStateException("Missing downloads.js", e); }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "NativeSave", Collections.singleton(ORIGIN),
                (view, messageData, source, mainFrame, reply) -> {
                    if (mainFrame && "https".equals(source.getScheme())
                        && "park03059-netizen.github.io".equals(source.getHost())
                        && (source.getPort() == -1 || source.getPort() == 443)
                        && isInternal(Uri.parse(web.getUrl() == null ? "" : web.getUrl()))) {
                        receiveDownload(messageData.getData());
                    }
                });
        }
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isInternal(uri)) return false;
                openExternal(uri);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorPanel.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (isInternal(Uri.parse(url))) web.evaluateJavascript(downloadScript, null);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                if (req.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse response) {
                if (req.isForMainFrame()) showError();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(pick, PICK_FILE); }
                catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback = null; toast("파일 선택 앱을 찾을 수 없습니다."); }
                return true;
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> {
            if (url.startsWith("blob:" + ORIGIN + "/")) {
                web.evaluateJavascript("window.fitPhotoSave && window.fitPhotoSave(" + JSONObject.quote(url) + ", 'photo')", null);
            } else { openExternal(Uri.parse(url)); }
        });
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::goBack);
        }
        hideSystemBars();
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }

    private boolean isInternal(Uri uri) {
        String path = uri.getPath();
        return "https".equals(uri.getScheme()) && "park03059-netizen.github.io".equals(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443)
            && path != null && path.startsWith("/parkminseok/");
    }

    private void receiveDownload(String data) {
        if (receivingDownload || pendingDownload != null) { toast("이전 파일 저장을 먼저 완료해 주세요."); return; }
        if (data == null || data.length() > MAX_BASE64 + 4096) { toast("파일이 너무 큽니다. 24MB 이하로 내보내 주세요."); return; }
        receivingDownload = true;
        io.execute(() -> {
            try {
                JSONObject json = new JSONObject(data);
                String mime = json.getString("mime");
                if (!mime.matches("image/(jpeg|png|webp|gif)") && !mime.equals("application/zip")) throw new IllegalArgumentException();
                String name = json.optString("name", "photo").replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
                if (name.length() > 180) name = name.substring(name.length() - 180);
                if (name.trim().isEmpty()) name = "photo";
                byte[] bytes = Base64.decode(json.getString("base64"), Base64.DEFAULT);
                String filename = name;
                runOnUiThread(() -> {
                    receivingDownload = false;
                    if (isDestroyed()) return;
                    pendingDownload = bytes;
                    Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    save.addCategory(Intent.CATEGORY_OPENABLE);
                    save.setType(mime);
                    save.putExtra(Intent.EXTRA_TITLE, filename);
                    try { startActivityForResult(save, SAVE_FILE); }
                    catch (ActivityNotFoundException e) { pendingDownload = null; toast("파일 저장 앱을 찾을 수 없습니다."); }
                });
            } catch (Exception e) {
                runOnUiThread(() -> { receivingDownload = false; toast("파일을 준비하지 못했습니다. 다시 시도해 주세요."); });
            }
        });
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && fileCallback != null) {
            Uri[] uris = null;
            if (result == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    uris = new Uri[data.getClipData().getItemCount()];
                    for (int i = 0; i < uris.length; i++) uris[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) uris = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(uris);
            fileCallback = null;
        }
        if (request == SAVE_FILE) {
            byte[] bytes = pendingDownload;
            pendingDownload = null;
            if (result == RESULT_OK && data != null && data.getData() != null) {
                if (bytes == null) { toast("저장 정보가 초기화됐습니다. 사이트에서 다시 내려받아 주세요."); return; }
                Uri destination = data.getData();
                io.execute(() -> {
                    try (OutputStream out = getContentResolver().openOutputStream(destination, "w")) {
                        if (out == null) throw new IllegalStateException();
                        out.write(bytes);
                        runOnUiThread(() -> toast("저장했습니다."));
                    } catch (Exception e) { runOnUiThread(() -> toast("파일을 저장하지 못했습니다. 다시 시도해 주세요.")); }
                });
            }
        }
    }

    private void openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if (!"https".equals(scheme) && !"http".equals(scheme) && !"mailto".equals(scheme) && !"tel".equals(scheme)) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
        catch (ActivityNotFoundException e) { toast("링크를 열 수 있는 앱이 없습니다."); }
    }
    private void showError() { errorPanel.setVisibility(View.VISIBLE); progress.setVisibility(View.GONE); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void goBack() { if (web.canGoBack()) web.goBack(); else finish(); }
    // API 33+ uses OnBackInvokedDispatcher registered in onCreate; this is the API 26-32 fallback.
    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation") @Override public void onBackPressed() { goBack(); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) hideSystemBars(); }
    @SuppressWarnings("deprecation") private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override protected void onPause() { web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        pendingDownload = null;
        web.stopLoading();
        web.destroy();
        io.shutdown();
        super.onDestroy();
    }
}
