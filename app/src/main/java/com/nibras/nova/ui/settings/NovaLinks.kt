package com.nibras.nova.ui.settings

/** Public links shown inside the app. Primary pages live on nibrascode.com; GitHub Pages keeps a mirror. */
object NovaLinks {
    const val WEBSITE = "https://nibrascode.com"
    const val CONTACT_EMAIL = "nibrascode@gmail.com"
    const val FEATURES = "$WEBSITE/nibrasnova/"
    const val PRIVACY_POLICY = "$WEBSITE/privacy/nibras-nova"
    /** Simplified Chinese policy (static page). */
    const val PRIVACY_POLICY_ZH = "$WEBSITE/privacy/nibras-nova-zh.html"

    /** Privacy policy URL for a UI language tag: Chinese ("zh", "zh-CN", "zh-Hans"…) gets the zh page. */
    fun privacyPolicyFor(language: String?): String =
        if (language?.lowercase()?.startsWith("zh") == true) PRIVACY_POLICY_ZH else PRIVACY_POLICY

    fun privacyPolicy(context: android.content.Context): String {
        val locales = context.resources.configuration.locales
        val lang = if (locales.isEmpty) java.util.Locale.getDefault().language else locales[0].language
        return privacyPolicyFor(lang)
    }

    /** Fallback mirror on GitHub Pages (repo docs/ folder). */
    const val PAGES_BASE = "https://nibrascode.github.io/nibrasnova"
    const val FEATURES_MIRROR = "$PAGES_BASE/"
    const val PRIVACY_POLICY_MIRROR = "$PAGES_BASE/privacy-policy.html"
    const val PRIVACY_POLICY_MIRROR_ZH = "$PAGES_BASE/privacy-policy-zh.html"
}
