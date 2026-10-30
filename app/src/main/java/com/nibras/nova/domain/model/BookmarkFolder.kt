package com.nibras.nova.domain.model

data class BookmarkFolder(
    val id: Long = 0L,
    val name: String,
    val parentFolderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
