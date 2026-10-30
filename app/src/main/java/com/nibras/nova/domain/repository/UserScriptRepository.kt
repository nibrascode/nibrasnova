package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.UserScriptItem
import kotlinx.coroutines.flow.Flow

interface UserScriptRepository {
    fun observeAllScripts(): Flow<List<UserScriptItem>>
    suspend fun ensureDefaultBuiltInScriptsSeeded()
    suspend fun getMatchingEnabledScriptsForUrl(url: String): List<UserScriptItem>
    suspend fun addOrUpdateScript(
        id: Long = 0L,
        name: String,
        matchDomainPattern: String,
        scriptType: ScriptInjectionType,
        codeContent: String,
        isEnabled: Boolean = true
    ): Long
    suspend fun toggleScriptEnabled(script: UserScriptItem, isEnabled: Boolean)
    suspend fun deleteScript(id: Long)
}
