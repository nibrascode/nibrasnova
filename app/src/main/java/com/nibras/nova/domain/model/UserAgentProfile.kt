package com.nibras.nova.domain.model

import com.nibras.nova.R
import com.nibras.nova.core.common.L

enum class UserAgentProfile(
    val id: String,
    val displayName: String,
    val userAgentString: String?,
    val forceWideDesktopViewport: Boolean
) {
    ANDROID_DEFAULT(
        id = "android_default",
        displayName = L.s(R.string.t_android_mobile_default),
        userAgentString = null,
        forceWideDesktopViewport = false
    ),
    WINDOWS_CHROME(
        id = "windows_chrome",
        displayName = "Windows 11 Desktop (Chrome 131)",
        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 NibrasNova/1.3",
        forceWideDesktopViewport = true
    ),
    MACOS_SAFARI(
        id = "macos_safari",
        displayName = "macOS Sequoia (Safari 18)",
        userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
        forceWideDesktopViewport = true
    ),
    LINUX_FIREFOX(
        id = "linux_firefox",
        displayName = "Linux x86_64 (Firefox 132)",
        userAgentString = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
        forceWideDesktopViewport = true
    ),
    IPHONE_IOS(
        id = "iphone_ios",
        displayName = "iPhone 16 Pro (iOS 18 Safari)",
        userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
        forceWideDesktopViewport = false
    ),
    IPAD_OS(
        id = "ipad_os",
        displayName = "iPad Pro (iPadOS 18)",
        userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
        forceWideDesktopViewport = true
    );

    companion object {
        fun fromId(id: String): UserAgentProfile =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ANDROID_DEFAULT
    }
}
