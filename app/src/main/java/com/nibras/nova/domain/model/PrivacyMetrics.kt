package com.nibras.nova.domain.model

data class PrivacyMetrics(
    val httpsUpgradesCount: Int = 0,
    val unsafeSchemesBlockedCount: Int = 0,
    val sslWarningsInterceptedCount: Int = 0,
    val dataClearsExecutedCount: Int = 0,
    val adsAndTrackersBlockedCount: Int = 0,
    val trackingParamsStrippedCount: Int = 0,
    val redirectChainsBlockedCount: Int = 0,
    val isMultiProfileIncognitoSupported: Boolean = false,
    val isSafeBrowsingSupported: Boolean = true
)
