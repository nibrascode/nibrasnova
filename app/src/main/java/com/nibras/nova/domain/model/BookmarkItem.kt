package com.nibras.nova.domain.model

data class BookmarkItem(
    val id: Long = 0L,
    val title: String,
    val url: String,
    val folderId: Long? = null,
    val faviconPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
