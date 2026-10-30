package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.UserScriptItem

@Entity(tableName = "user_scripts")
data class UserScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val matchDomainPattern: String,
    val scriptType: String,
    val codeContent: String,
    val isEnabled: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = UserScriptItem(
        id = id,
        name = name,
        matchDomainPattern = matchDomainPattern,
        scriptType = runCatching { ScriptInjectionType.valueOf(scriptType) }.getOrDefault(ScriptInjectionType.JAVASCRIPT),
        codeContent = codeContent,
        isEnabled = isEnabled,
        createdAt = createdAt
    )
}
