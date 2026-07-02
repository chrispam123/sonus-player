package com.sonus.player.domain.model

data class PlaybackState(
    val isPlaying: Boolean,
    val currentTrack: Track?,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val sleepTimerRemainingMs: Long?
)

data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long
)

data class SavedPlaybackState(
    val trackId: Long,
    val positionMs: Long,
    val queueTrackIds: List<Long>,
    val queueIndex: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode
)

enum class RepeatMode { OFF, ONE, ALL }
