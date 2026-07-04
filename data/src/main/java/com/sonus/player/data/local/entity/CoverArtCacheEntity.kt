package com.sonus.player.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cover_art_cache")
data class CoverArtCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "album_id")
    val albumId: Long,

    @ColumnInfo(name = "source_type")
    val sourceType: String,         // "embedded", "lastfm", "musicbrainz", "generated"

    val url: String?,               // URL or local file path

    val colors: String?,            // JSON array of colors for generated gradients

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long             // epoch ms
)
