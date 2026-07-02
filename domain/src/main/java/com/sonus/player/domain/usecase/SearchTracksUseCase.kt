package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow

class SearchTracksUseCase(private val repository: MusicRepository) {
    operator fun invoke(query: String): Flow<List<Track>> = repository.searchTracks(query)
}
