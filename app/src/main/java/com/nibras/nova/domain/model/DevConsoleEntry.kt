package com.nibras.nova.domain.model

enum class ConsoleLogLevel {
    LOG,
    INFO,
    WARN,
    ERROR
}

data class DevConsoleEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: ConsoleLogLevel,
    val message: String,
    val sourceId: String,
    val lineNumber: Int
)

data class NetworkResourceLog(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val wasBlockedByAdBlocker: Boolean
)
