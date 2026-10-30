package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.BookmarkItem

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["url"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val folderId: Long? = null,
    val faviconPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = BookmarkItem(
        id = id,
        title = title,
        url = url,
        folderId = folderId,
        faviconPath = faviconPath,
        createdAt = createdAt
    )
}
