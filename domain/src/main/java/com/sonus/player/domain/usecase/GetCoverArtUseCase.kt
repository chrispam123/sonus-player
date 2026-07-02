package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.CoverArtResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.CoverArtRepository

class GetCoverArtUseCase(private val repository: CoverArtRepository) {
    suspend operator fun invoke(track: Track): CoverArtResult = repository.getCoverArt(track)
}
