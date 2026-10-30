package com.nibras.nova.domain.usecase

import com.nibras.nova.domain.repository.HistoryRepository
import javax.inject.Inject

class RecordHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        title: String,
        url: String,
        faviconPath: String?,
        isPrivateSession: Boolean
    ) {
        historyRepository.recordVisit(
            title = title,
            url = url,
            faviconPath = faviconPath,
            isPrivateSession = isPrivateSession
        )
    }
}
