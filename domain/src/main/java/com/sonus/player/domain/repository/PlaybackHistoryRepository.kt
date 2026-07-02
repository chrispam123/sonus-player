package com.sonus.player.domain.repository

import com.sonus.player.domain.model.HistoryEntry
import com.sonus.player.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntry>>
    suspend fun addToHistory(track: Track)
    suspend fun clearHistory()
}
