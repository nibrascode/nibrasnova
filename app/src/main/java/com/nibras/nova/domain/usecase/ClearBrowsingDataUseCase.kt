package com.nibras.nova.domain.usecase

import com.nibras.nova.domain.repository.PrivacyRepository
import javax.inject.Inject

class ClearBrowsingDataUseCase @Inject constructor(
    private val privacyRepository: PrivacyRepository
) {
    suspend operator fun invoke(
        clearHistory: Boolean,
        clearCookiesAndSiteData: Boolean,
        clearCache: Boolean,
        clearSitePermissions: Boolean
    ) {
        privacyRepository.clearBrowsingData(
            clearHistory = clearHistory,
            clearCookiesAndSiteData = clearCookiesAndSiteData,
            clearCache = clearCache,
            clearSitePermissions = clearSitePermissions
        )
    }
}
