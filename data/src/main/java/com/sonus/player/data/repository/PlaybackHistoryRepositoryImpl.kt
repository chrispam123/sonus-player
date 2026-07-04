package com.sonus.player.data.repository

import com.sonus.player.data.local.dao.HistoryDao
import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.HistoryEntity
import com.sonus.player.domain.model.HistoryEntry
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.PlaybackHistoryRepository
import com.sonus.player.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val trackDao: TrackDao
) : PlaybackHistoryRepository {

    companion object {
        private const val MAX_HISTORY_SIZE = 100
    }

    override fun getRecentHistory(limit: Int): Flow<List<HistoryEntry>> =
        historyDao.getRecentHistory(limit).map { entities ->
            entities.mapNotNull { entity ->
                val track = trackDao.getTrackById(entity.trackId)?.toDomain()
                if (track != null) {
                    HistoryEntry(track = track, playedAt = entity.playedAt)
                } else null
            }
        }

    override suspend fun addToHistory(track: Track) {
        historyDao.insert(
            HistoryEntity(trackId = track.id, playedAt = System.currentTimeMillis())
        )
        // Trim to keep only last 100 entries
        historyDao.trimToLimit(MAX_HISTORY_SIZE)
    }

    override suspend fun clearHistory() = historyDao.clearAll()
}
