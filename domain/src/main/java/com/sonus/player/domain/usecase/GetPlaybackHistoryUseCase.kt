package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.HistoryEntry
import com.sonus.player.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetPlaybackHistoryUseCase(private val repository: PlaybackHistoryRepository) {
    operator fun invoke(limit: Int = 100): Flow<List<HistoryEntry>> =
        repository.getRecentHistory(limit)
}
