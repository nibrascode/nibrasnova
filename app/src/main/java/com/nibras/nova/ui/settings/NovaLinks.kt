package com.nibras.nova.ui.settings

/** Public links shown inside the app. Primary pages live on nibrascode.com; GitHub Pages keeps a mirror. */
object NovaLinks {
    const val WEBSITE = "https://nibrascode.com"
    const val CONTACT_EMAIL = "nibrascode@gmail.com"
    const val FEATURES = "$WEBSITE/nibrasnova/"
    const val PRIVACY_POLICY = "$WEBSITE/privacy/nibras-nova"

    /** Fallback mirror on GitHub Pages (repo docs/ folder). */
    const val PAGES_BASE = "https://nibrascode.github.io/nibrasnova"
    const val FEATURES_MIRROR = "$PAGES_BASE/"
    const val PRIVACY_POLICY_MIRROR = "$PAGES_BASE/privacy-policy.html"
}
