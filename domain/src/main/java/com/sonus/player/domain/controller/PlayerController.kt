package com.sonus.player.domain.controller

import com.sonus.player.domain.model.PlaybackProgress
import com.sonus.player.domain.model.PlaybackState
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val playbackState: StateFlow<PlaybackState>
    val currentTrack: StateFlow<Track?>
    val progress: StateFlow<PlaybackProgress>
    val queue: StateFlow<List<Track>>

    fun play(track: Track)
    fun playQueue(tracks: List<Track>, startIndex: Int = 0)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun next()
    fun previous()
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}
