package com.nibras.nova.domain.model

enum class ScriptInjectionType(val label: String) {
    JAVASCRIPT("UserScript (JavaScript)"),
    CSS("UserStyle (Custom CSS)")
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
