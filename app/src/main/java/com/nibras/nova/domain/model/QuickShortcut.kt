package com.nibras.nova.domain.model

data class QuickShortcut(
    val id: Long = 0L,
    val title: String,
    val url: String,
    val iconColorHex: String = "#00E5FF",
    val sortOrder: Int = 0,
    val isPinned: Boolean = true
)
