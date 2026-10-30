package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.BookmarkFolder

@Entity(
    tableName = "bookmark_folders",
    indices = [Index(value = ["parentFolderId"])]
)
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val parentFolderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = BookmarkFolder(
        id = id,
        name = name,
        parentFolderId = parentFolderId,
        createdAt = createdAt
    )
}
