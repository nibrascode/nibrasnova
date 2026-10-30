package com.nibras.nova.core.security

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

enum class DnsOverHttpsProvider(
    val id: String,
    val displayName: String,
    val endpointTemplate: String,
    val description: String
) {
    SYSTEM_DEFAULT(
        id = "system",
        displayName = L.s(R.string.t_system_default_dns),
        endpointTemplate = "",
        description = L.s(R.string.t_uses_standard_android_private_dns_networ)
    ),
    CLOUDFLARE_1111(
        id = "cloudflare",
        displayName = "Cloudflare (1.1.1.1)",
        endpointTemplate = "https://cloudflare-dns.com/dns-query",
        description = L.s(R.string.t_fast_global_dns_over_https_resolver_with)
    ),
    QUAD9_SECURITY(
        id = "quad9",
        displayName = "Quad9 Security (9.9.9.9)",
        endpointTemplate = "https://dns.quad9.net/dns-query",
        description = L.s(R.string.t_swiss_privacy_foundation_doh_blocking_kn)
    ),
    ADGUARD_DNS(
        id = "adguard",
        displayName = "AdGuard DoH",
        endpointTemplate = "https://dns.adguard-dns.com/dns-query",
        description = L.s(R.string.t_encrypted_dns_that_filters_advertising_a)
    );

    companion object {
        fun fromId(id: String): DnsOverHttpsProvider =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM_DEFAULT
    }
}

@Singleton
class ProxyDoHManager @Inject constructor() {

    private val directExecutor = Executor { command -> command.run() }

    val isProxyOverrideSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    fun applyCustomProxyOrClear(customProxyHostPort: String): Result<Boolean> {
        if (!isProxyOverrideSupported) {
            return Result.failure(UnsupportedOperationException(L.s(R.string.t_webviewfeature_proxy_override_is_not_sup)))
        }
        return try {
            val clean = customProxyHostPort.trim()
            if (clean.isBlank()) {
                ProxyController.getInstance().clearProxyOverride(directExecutor) {}
                Result.success(false)
            } else {
                val proxyConfig = ProxyConfig.Builder()
                    .addProxyRule(clean)
                    .addBypassRule("localhost")
                    .addBypassRule("127.0.0.1")
                    .build()
                ProxyController.getInstance().setProxyOverride(proxyConfig, directExecutor) {}
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
