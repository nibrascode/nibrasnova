package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.QuickShortcut

@Entity(tableName = "quick_shortcuts")
data class QuickShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val iconColorHex: String = "#00E5FF",
    val sortOrder: Int = 0,
    val isPinned: Boolean = true
) {
    fun toDomain() = QuickShortcut(
        id = id,
        title = title,
        url = url,
        iconColorHex = iconColorHex,
        sortOrder = sortOrder,
        isPinned = isPinned
    )
}
