package com.sonus.player.domain.repository

import com.sonus.player.domain.model.CoverArtResult
import com.sonus.player.domain.model.Track

interface CoverArtRepository {
    suspend fun getCoverArt(track: Track): CoverArtResult
    suspend fun clearCache()
}
