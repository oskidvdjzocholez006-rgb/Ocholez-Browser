package com.ocholez.browser;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    LinearLayout root, toolbar, tabStrip;
    EditText address;
    FrameLayout pageFrame;
    TextView status;
    ArrayList<BrowserTab> tabs = new ArrayList<>();
    int active = 0;
    boolean dark = false;
    boolean privateMode = false;
    SharedPreferences prefs;

    static class BrowserTab {
        WebView web;
        String title = "New tab";
        String url = "https://www.google.com";
        boolean isPrivate;
        BrowserTab(WebView w, boolean p) { web = w; isPrivate = p; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ocholez", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        buildUi();
        addTab(false, "https://www.google.com");
    }

    int bg() { return dark ? Color.rgb(18,21,30) : Color.WHITE; }
    int panel() { return dark ? Color.rgb(29,34,47) : Color.rgb(242,244,248); }
    int fg() { return dark ? Color.WHITE : Color.rgb(25,29,38); }
    GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(12)); return d;
    }
    int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
    TextView button(String label, Runnable action) {
        TextView b = new TextView(this); b.setText(label); b.setTextColor(fg());
        b.setTextSize(14); b.setGravity(Gravity.CENTER); b.setPadding(dp(12),dp(10),dp(12),dp(10));
        b.setBackground(rounded(panel())); b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
        lp.setMargins(dp(3),dp(3),dp(3),dp(3)); b.setLayoutParams(lp); return b;
    }
    void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        setContentView(root);
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL); brand.setPadding(dp(10),dp(6),dp(10),dp(4));
        TextView logo = new TextView(this); logo.setText("◉  Ocholez Browser"); logo.setTextColor(fg()); logo.setTextSize(19); logo.setTypeface(null,1);
        brand.addView(logo, new LinearLayout.LayoutParams(0,dp(42),1));
        brand.addView(button("＋", () -> addTab(privateMode, "https://www.google.com")));
        root.addView(brand);
        HorizontalScrollView hsv = new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false);
        tabStrip = new LinearLayout(this); tabStrip.setPadding(dp(5),0,dp(5),dp(3)); hsv.addView(tabStrip);
        root.addView(hsv);
        toolbar = new LinearLayout(this); toolbar.setPadding(dp(5),dp(4),dp(5),dp(4)); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(button("‹", () -> { if (current().web.canGoBack()) current().web.goBack(); }));
        toolbar.addView(button("›", () -> { if (current().web.canGoForward()) current().web.goForward(); }));
        address = new EditText(this); address.setSingleLine(true); address.setTextSize(14); address.setTextColor(fg());
        address.setHintTextColor(dark ? 0xFFB0B7C5 : 0xFF666666); address.setHint("Search or enter website");
        address.setImeOptions(EditorInfo.IME_ACTION_GO); address.setPadding(dp(10),0,dp(10),0); address.setBackground(rounded(panel()));
        toolbar.addView(address, new LinearLayout.LayoutParams(0,dp(42),1));
        address.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode()==66)) {
                loadInput(address.getText().toString()); hideKeyboard(); return true;
            } return false;
        });
        toolbar.addView(button("Go", () -> { loadInput(address.getText().toString()); hideKeyboard(); }));
        root.addView(toolbar);
        LinearLayout tools = new LinearLayout(this); tools.setPadding(dp(4),0,dp(4),dp(4));
        tools.addView(button("☆ Save", this::saveBookmark));
        tools.addView(button("Bookmarks", this::showBookmarks));
        tools.addView(button("History", this::showHistory));
        tools.addView(button(privateMode ? "Private: ON" : "Private", this::togglePrivate));
        tools.addView(button(dark ? "☀" : "☾", this::toggleDark));
        root.addView(tools);
        pageFrame = new FrameLayout(this); root.addView(pageFrame, new LinearLayout.LayoutParams(-1,0,1));
        status = new TextView(this); status.setText("Ready"); status.setTextColor(fg()); status.setTextSize(11);
        status.setPadding(dp(10),dp(4),dp(10),dp(4)); root.addView(status);
    }
    BrowserTab current() { return tabs.get(active); }
    void addTab(boolean priv, String url) {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.WHITE);
        WebSettings s = w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(!priv);
        s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        if (priv) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); w.clearCache(true); w.clearHistory(); CookieManager.getInstance().setAcceptCookie(false); }
        else { CookieManager.getInstance().setAcceptCookie(true); }
        BrowserTab t = new BrowserTab(w, priv);
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return false; }
            @Override public void onPageStarted(WebView view, String u, android.graphics.Bitmap icon) {
                t.url = u; if (activeTab(t)) address.setText(u); status.setText("Loading…");
            }
            @Override public void onPageFinished(WebView view, String u) {
                t.url = u; t.title = view.getTitle() == null ? u : view.getTitle();
                if (activeTab(t)) { address.setText(u); status.setText(t.isPrivate ? "Private browsing • history not saved" : "Page loaded"); refreshTabs(); }
                if (!t.isPrivate) addHistory(t.title, u);
                if (android.os.Build.VERSION.SDK_INT >= 29) view.getSettings().setForceDark(dark ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (activeTab(t)) status.setText(progress < 100 ? "Loading " + progress + "%" : (t.isPrivate ? "Private browsing" : "Ready"));
            }
            @Override public void onReceivedTitle(WebView view, String title) { t.title = title; refreshTabs(); }
        });
        w.setDownloadListener((u, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(u));
                req.setTitle(URLUtil.guessFileName(u, contentDisposition, mimeType));
                req.setDescription("Downloading with Ocholez Browser");
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String cookies = CookieManager.getInstance().getCookie(u);
                if (cookies != null) req.addRequestHeader("Cookie", cookies);
                if (userAgent != null) req.addRequestHeader("User-Agent", userAgent);
                req.setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(u, contentDisposition, mimeType));
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch(Exception e) { Toast.makeText(this, "Could not start download", Toast.LENGTH_SHORT).show(); }
        });
        tabs.add(t); active = tabs.size()-1;
        pageFrame.addView(w, new FrameLayout.LayoutParams(-1,-1));
        showActive(); refreshTabs(); w.loadUrl(url);
    }
    boolean activeTab(BrowserTab t) { return !tabs.isEmpty() && current()==t; }
    void showActive() {
        pageFrame.removeAllViews(); pageFrame.addView(current().web,new FrameLayout.LayoutParams(-1,-1));
        address.setText(current().url); refreshTabs();
    }
    void refreshTabs() {
        if (tabStrip == null) return;
        tabStrip.removeAllViews();
        for (int i=0;i<tabs.size();i++) {
            final int idx=i; BrowserTab t=tabs.get(i);
            String label=(i==active?"● ":"")+ (t.isPrivate?"🔒 ":"") + (t.title.length()>13?t.title.substring(0,13)+"…":t.title) + " ×";
            TextView b=button(label, () -> closeTab(idx));
            b.setBackground(rounded(i==active ? (dark?0xFF414B70:0xFFDDE4FF) : panel()));
            b.setOnClickListener(v -> { active=idx; showActive(); });
            b.setOnLongClickListener(v -> { closeTab(idx); return true; });
            tabStrip.addView(b);
        }
    }
    void closeTab(int idx) {
        if (tabs.size()==1) { current().web.loadUrl("https://www.google.com"); return; }
        BrowserTab t=tabs.remove(idx); pageFrame.removeView(t.web); t.web.stopLoading(); t.web.destroy();
        if (idx<active) active--; else if (idx==active) active=Math.min(active,tabs.size()-1);
        showActive();
    }
    void loadInput(String raw) {
        String q=raw.trim(); if(q.isEmpty()) return;
        String target;
        if(q.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) target=q;
        else if(q.contains(".") && !q.contains(" ") && !q.startsWith("www.")) target="https://"+q;
        else if(q.startsWith("www.")) target="https://"+q;
        else target="https://www.google.com/search?q="+Uri.encode(q);
        current().web.loadUrl(target);
    }
    void saveBookmark() {
        BrowserTab t=current(); if(t.url==null || t.url.equals("about:blank")) return;
        try {
            JSONArray a=new JSONArray(prefs.getString("bookmarks","[]")); JSONObject o=new JSONObject();
            o.put("title",t.title); o.put("url",t.url); a.put(o); prefs.edit().putString("bookmarks",a.toString()).apply();
            Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();
        } catch(Exception e) {}
    }
    void addHistory(String title,String url) {
        try {
            JSONArray a=new JSONArray(prefs.getString("history","[]")); JSONObject o=new JSONObject();
            o.put("title",title==null?url:title); o.put("url",url); o.put("time",System.currentTimeMillis()); a.put(o);
            while(a.length()>300) a.remove(0);
            prefs.edit().putString("history",a.toString()).apply();
        } catch(Exception e) {}
    }
    void showList(String key, String heading, boolean canClear) {
        try {
            JSONArray a=new JSONArray(prefs.getString(key,"[]"));
            ArrayList<String> labels=new ArrayList<>(); ArrayList<String> urls=new ArrayList<>();
            for(int i=a.length()-1;i>=0;i--) { JSONObject o=a.getJSONObject(i); labels.add(o.optString("title",o.optString("url"))+"\n"+o.optString("url")); urls.add(o.optString("url")); }
            if(labels.isEmpty()) labels.add("Nothing saved yet");
            AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(heading).setItems(labels.toArray(new String[0]),(d,which)->{ if(which<urls.size()) current().web.loadUrl(urls.get(which)); });
            if(canClear) b.setNeutralButton("Clear", (d,w)->{prefs.edit().putString(key,"[]").apply(); Toast.makeText(this,"Cleared",Toast.LENGTH_SHORT).show();});
            b.setNegativeButton("Close",null).show();
        } catch(Exception e) { new AlertDialog.Builder(this).setMessage("No items yet").setPositiveButton("OK",null).show(); }
    }
    void showBookmarks(){ showList("bookmarks","Bookmarks",true); }
    void showHistory(){ if(!privateMode) showList("history","Browsing history",true); else Toast.makeText(this,"History is disabled in private mode",Toast.LENGTH_SHORT).show(); }
    void togglePrivate() {
        privateMode=!privateMode;
        if(privateMode) addTab(true,"https://www.google.com");
        else { CookieManager.getInstance().setAcceptCookie(true); addTab(false,"https://www.google.com"); }
        rebuildThemeUi();
    }
    void toggleDark(){ dark=!dark; prefs.edit().putBoolean("dark",dark).apply(); rebuildThemeUi(); for(BrowserTab t:tabs) if(Build.VERSION.SDK_INT>=29) t.web.getSettings().setForceDark(dark?WebSettings.FORCE_DARK_ON:WebSettings.FORCE_DARK_OFF); }
    void rebuildThemeUi() {
        // Rebuild controls while retaining existing WebViews and selected tab.
        int oldActive=active;
        root.removeAllViews(); buildUi();
        active=oldActive;
        for(BrowserTab t:tabs) { if(t.web.getParent()!=null) ((android.view.ViewGroup)t.web.getParent()).removeView(t.web); }
        showActive();
    }
    void hideKeyboard() {
        try { ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0); address.clearFocus(); } catch(Exception e) {}
    }
    @Override public void onBackPressed() {
        if(current().web.canGoBack()) current().web.goBack();
        else if(tabs.size()>1) closeTab(active);
        else super.onBackPressed();
    }
}
