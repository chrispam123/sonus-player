package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.LyricsResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.LyricsRepository

class GetLyricsUseCase(private val repository: LyricsRepository) {
    suspend operator fun invoke(track: Track): LyricsResult = repository.getLyrics(track)
}
