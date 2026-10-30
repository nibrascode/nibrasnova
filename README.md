# Nibras Nova

**Advanced Premium Android Web Browser**

Nibras Nova is a modern, feature-rich Android browser built with Kotlin and Jetpack Compose. It focuses on privacy, performance, and advanced browsing tools.

**Package:** `com.nibras.nova`  
**Version:** 2.0.0-nova-flagship  
**Min SDK:** 26 | **Target/Compile SDK:** 35

## Features

- Multi-tab system with private browsing separation
- Built-in Ad & Tracker Blocker
- Reader Mode with offline saving
- Download Center with pause/resume
- QR Scanner & Generator
- Privacy Dashboard & Incognito Vault (biometric lock)
- Split-screen dual WebView
- Picture-in-Picture video
- Full-page screenshot
- On-device article summarizer
- RSS/Atom news reader
- Encrypted backup & restore
- Custom UserScripts / CSS
- DevTools console & DOM inspector
- And much more (see DEVELOPMENT_LOG.md)

## Build

This project uses Gradle Version Catalog and Android Gradle Plugin 8.7.2.

### Local build

```bash
./gradlew assembleDebug
# or
./gradlew assembleRelease
```

APK will be in `app/build/outputs/apk/`.

### GitHub Actions

Push to `main` triggers automatic debug APK build.  
Download the artifact from the Actions tab.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

## License

Proprietary / Nibras Code
