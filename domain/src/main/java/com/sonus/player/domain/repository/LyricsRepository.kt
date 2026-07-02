package com.sonus.player.domain.repository

import com.sonus.player.domain.model.LyricsResult
import com.sonus.player.domain.model.SyncedLyricsResult
import com.sonus.player.domain.model.Track

interface LyricsRepository {
    suspend fun getLyrics(track: Track): LyricsResult
    suspend fun getSyncedLyrics(track: Track): SyncedLyricsResult
}
