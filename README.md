# Nibras Nova — Advanced Premium Android Web Browser (`v2.0.0-nova-flagship`)

**Brand Ecosystem:** Nibras Code  
**Package:** `com.nibras.nova`  
**Stack:** Kotlin 2.0.21, Jetpack Compose (Material 3), AndroidX WebKit (`1.12.1`), Room (`2.6.1`), DataStore (`1.1.1`), Hilt (`2.52`), CameraX + ZXing, ML Kit Translate, AndroidX Biometric.

## Key Engineering Highlights (Stages 1 – 14)

- **Hardened WebView Engine & Bounded LRU Pool**: Caps live `WebView` instances in RAM at `MAX_LIVE_WEBVIEWS = 4`, serializing cold tabs via `saveState(Bundle)` and capturing WebP previews to eliminate native Chromium OOM crashes.
- **Zero-Trust Security & Privacy Shield**:
  - Built-in **Ad & Tracker Blocker** (`AdBlockerEngine.kt` in `shouldInterceptRequest`).
  - **Marketing & Tracking URL Parameter Stripper** (`utm_*`, `fbclid`, `gclid`, `mc_cid`).
  - **Canvas, WebGL & AudioContext Anti-Fingerprinting Shield** (`AntiFingerprintInjector.kt`).
  - **Homograph / IDN Punycode Phishing Detector** (`HomographAttackDetector.kt`).
  - **Rapid Cross-Domain Redirect Chain Guard** (`NovaWebViewClient.kt`).
  - **DNS-over-HTTPS (DoH) & Custom Proxy Controller** (`ProxyDoHManager.kt`).
  - **Biometric Private Tabs & Encrypted Private Media/Bookmark Vault** (`PrivateVaultScreen.kt` + `PasswordGeneratorHelper.kt`).
  - **AES-256-GCM + PBKDF2-HMAC-SHA256 Encrypted `.novabackup` Export & Restore** (`EncryptedBackupManager.kt`).
  - **One-Tap Emergency Panic Button** (destroys live WebViews, scrubs clipboard, wipes cookies/cache/history, and exits).
- **Desktop-Class Power Tools on Android**:
  - **F12 Web Developer Console, JS REPL, Network Monitor, Live DOM `designMode` Editor & Storage Inspector** (`DevToolsSheet.kt`).
  - **Custom UserScripts (Tampermonkey JS) & Custom CSS Stylesheet Injector** (`UserScriptsScreen.kt`).
  - **Eyedropper Color Palette Extractor & CSS Typography Inspector** (`ColorFontInspectorSheet.kt`).
  - **Native JSON / XML / API Response Formatter** (`JsonXmlFormatterSheet.kt`).
  - **Dual Split-Screen WebView Mode**, **Floating Picture-in-Picture (PiP) Video**, **Full-Page Scrolling Screenshot Capture**, and **Nova Omni-Command Palette (`Ctrl+K`)**.
  - **Local Wi-Fi / LAN Peer-to-Peer QR Sync Server** (`LocalLanSyncServer.kt`) for instant serverless tab & bookmark transfer across devices.
- **Global Localization**: Full UI resource support for **English (`en`)**, **Azerbaijani (`az`)**, **Turkish (`tr`)**, **Russian (`ru`)**, and **Arabic (`ar` RTL)**.
