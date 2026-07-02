package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.SyncedLyricsResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.LyricsRepository

class GetSyncedLyricsUseCase(private val repository: LyricsRepository) {
    suspend operator fun invoke(track: Track): SyncedLyricsResult = repository.getSyncedLyrics(track)
}
