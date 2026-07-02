package com.sonus.player.domain.model

sealed class LyricsResult {
    data class Found(val text: String) : LyricsResult()
    data object NotFound : LyricsResult()
    data class Error(val message: String) : LyricsResult()
}

sealed class SyncedLyricsResult {
    data class Found(val lines: List<SyncedLine>) : SyncedLyricsResult()
    data object NotFound : SyncedLyricsResult()
    data class Error(val message: String) : SyncedLyricsResult()
}

data class SyncedLine(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long?
)
