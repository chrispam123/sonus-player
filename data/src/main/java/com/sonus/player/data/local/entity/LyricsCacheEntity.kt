package com.sonus.player.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "plain_text")
    val plainText: String?,             // Plain text lyrics (from Genius)

    @ColumnInfo(name = "synced_lyrics")
    val syncedLyrics: String?,          // LRC format string (from LRCLIB)

    val source: String,                 // "lrclib", "genius"

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long                 // epoch ms
)
