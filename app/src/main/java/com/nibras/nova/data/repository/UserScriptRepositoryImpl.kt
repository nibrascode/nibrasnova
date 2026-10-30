package com.nibras.nova.data.repository

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.net.Uri
import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.UserScriptDao
import com.nibras.nova.data.local.entity.UserScriptEntity
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.UserScriptItem
import com.nibras.nova.domain.repository.UserScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserScriptRepositoryImpl @Inject constructor(
    private val userScriptDao: UserScriptDao,
    private val dispatchers: DispatcherProvider
) : UserScriptRepository {

    override fun observeAllScripts(): Flow<List<UserScriptItem>> =
        userScriptDao.observeAllScripts().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDefaultBuiltInScriptsSeeded() = withContext(dispatchers.io) {
        if (userScriptDao.getCount() == 0) {
            userScriptDao.insertScript(
                UserScriptEntity(
                    name = L.s(R.string.t_universal_cookie_banner_hider_css),
                    matchDomainPattern = "*",
                    scriptType = ScriptInjectionType.CSS.name,
                    codeContent = "#onetrust-consent-sdk, .cc-window, #cookie-banner, .cookie-consent-banner { display: none !important; }",
                    isEnabled = false
                )
            )
            userScriptDao.insertScript(
                UserScriptEntity(
                    name = L.s(R.string.t_disable_page_text_selection_locks_js),
                    matchDomainPattern = "*",
                    scriptType = ScriptInjectionType.JAVASCRIPT.name,
                    codeContent = "document.querySelectorAll('*').forEach(el => { el.style.userSelect = 'auto'; }); document.oncopy = null; document.oncontextmenu = null;",
                    isEnabled = false
                )
            )
        }
    }

    override suspend fun getMatchingEnabledScriptsForUrl(url: String): List<UserScriptItem> =
        withContext(dispatchers.io) {
            val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
            if (host.isBlank()) return@withContext emptyList()
            userScriptDao.getEnabledScriptsOnce()
                .map { it.toDomain() }
                .filter { it.matchesHost(host) }
        }

    override suspend fun addOrUpdateScript(
        id: Long,
        name: String,
        matchDomainPattern: String,
        scriptType: ScriptInjectionType,
        codeContent: String,
        isEnabled: Boolean
    ): Long = withContext(dispatchers.io) {
        val entity = UserScriptEntity(
            id = id,
            name = name.trim().ifBlank { L.s(R.string.t_custom_script) },
            matchDomainPattern = matchDomainPattern.trim().ifBlank { "*" },
            scriptType = scriptType.name,
            codeContent = codeContent,
            isEnabled = isEnabled
        )
        userScriptDao.insertScript(entity)
    }

    override suspend fun toggleScriptEnabled(script: UserScriptItem, isEnabled: Boolean) =
        withContext(dispatchers.io) {
            userScriptDao.updateScript(
                UserScriptEntity(
                    id = script.id,
                    name = script.name,
                    matchDomainPattern = script.matchDomainPattern,
                    scriptType = script.scriptType.name,
                    codeContent = script.codeContent,
                    isEnabled = isEnabled,
                    createdAt = script.createdAt
                )
            )
        }

    override suspend fun deleteScript(id: Long) = withContext(dispatchers.io) {
        userScriptDao.deleteScript(id)
    }
}
