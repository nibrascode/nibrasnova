package com.nibras.nova.domain.model

import com.nibras.nova.R
import com.nibras.nova.core.common.L

enum class ScriptInjectionType(val label: String) {
    JAVASCRIPT(L.s(R.string.t_userscript_javascript)),
    CSS(L.s(R.string.t_userstyle_custom_css))
}

data class UserScriptItem(
    val id: Long = 0L,
    val name: String,
    val matchDomainPattern: String = "*",
    val scriptType: ScriptInjectionType = ScriptInjectionType.JAVASCRIPT,
    val codeContent: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun matchesHost(currentHost: String): Boolean {
        val pattern = matchDomainPattern.trim().lowercase()
        if (pattern == "*" || pattern.isEmpty()) return true
        val host = currentHost.trim().lowercase()
        return host == pattern || host.endsWith(".$pattern")
    }
}
