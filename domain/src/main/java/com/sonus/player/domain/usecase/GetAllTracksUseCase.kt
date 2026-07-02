package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow

class GetAllTracksUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Track>> = repository.getAllTracks()
}
