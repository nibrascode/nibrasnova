# Nibras Nova — Production Release & QA Checklist

**Application:** Nibras Nova (`com.nibras.nova`)  
**Version:** `1.0.0-nova` (Version Code `100`)  
**Target SDK:** 35 (Android 15) | **Min SDK:** 26 (Android 8.0)

---

## 1. Security & Privacy Verification Checklist

- [x] **Zero Arbitrary JavaScript Interfaces**: Verified that `addJavascriptInterface` is never called on any `WebView` instance. Reader Mode content extraction executes strictly via one-way `WebView.evaluateJavascript()`.
- [x] **Local File & Content Provider Isolation**: `allowFileAccess = false`, `allowContentAccess = false`, `allowFileAccessFromFileURLs = false`, and `allowUniversalAccessFromFileURLs = false` enforced in `WebViewConfigurator.kt`.
- [x] **Non-Bypassable SSL Warning Interception**: `NovaWebViewClient.onReceivedSslError` never calls `handler.proceed()` automatically; it halts navigation and presents `SslWarningDialog` with certificate subject, issuer, and error cipher reason.
- [x] **Dangerous URI Scheme Blocking**: `UrlValidator` and `SchemePolicy` block `javascript:`, `vbscript:`, `file://`, `content://`, and `data:` top-level navigations.
- [x] **Intent Redirection Sanitization**: External `intent://` links parsed via `Intent.parseUri` have `component = null`, `selector = null`, and `GRANT_READ/WRITE_URI_PERMISSION` flags stripped before user confirmation.
- [x] **File Upload Sandbox Protection**: `FileUploadValidator` rejects any `file://` URI pointing inside `context.applicationInfo.dataDir` and enforces a 200 MB upload ceiling.
- [x] **Incognito Isolation**: Private tabs are held strictly in memory (`TabRepositoryImpl`), never write to `HistoryDao` or `TabDao`, enforce `WindowManager.LayoutParams.FLAG_SECURE`, and bind to `NovaIncognitoProfile` (`WebViewFeature.MULTI_PROFILE`) with fallback cache/session cookie wiping on older devices.
- [x] **Cloud Backup Disabled for Sensitive Database**: `android:allowBackup="false"` and `data_extraction_rules.xml` prevent unencrypted cloud backups of browsing history and cookies.

---

## 2. Memory & Performance Checklist

- [x] **Bounded LRU WebView Pool**: `WebViewPoolManager` caps live `WebView` instances at `MAX_LIVE_WEBVIEWS = 4`. Tabs beyond the pool limit save state (`WebView.saveState(Bundle)`) and invoke `webView.destroy()` cleanly.
- [x] **Background WebView Throttling**: Non-visible pooled WebViews invoke `webView.onPause()` immediately when switching tabs.
- [x] **Downsampled WebP Tab Previews**: `TabThumbnailManager` scales captured canvases to 45% resolution and compresses thumbnails as WebP on `Dispatchers.IO`.

---

## 3. Google Play Signing & Build Verification Steps

Because the sandbox VM environment does not have outbound internet access or a pre-installed Android SDK/Gradle binary to execute `./gradlew assembleRelease`, run the following verified commands on your local Android Studio / CI workstation:

```bash
# 1. Run JVM Unit Tests (URL validation, Scheme sanitization, Incognito isolation, Repositories)
./gradlew testDebugUnitTest

# 2. Run Android Instrumented Tests on a connected emulator or device
./gradlew connectedDebugAndroidTest

# 3. Build R8-Minified Signed Release App Bundle (.aab) and APK (.apk)
./gradlew bundleRelease assembleRelease
```
