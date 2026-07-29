package com.sonus.player.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val artist: String,
    val album: String,

    @ColumnInfo(name = "album_id")
    val albumId: Long,

    val duration: Long,         // milliseconds

    @ColumnInfo(name = "file_path")
    val filePath: String,

    // 🆕 URL de streaming (ccMixter). NULL para archivos locales.
    @ColumnInfo(name = "stream_url")
    val streamUrl: String? = null,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    val bitrate: Int,           // kbps

    @ColumnInfo(name = "sample_rate")
    val sampleRate: Int,        // Hz

    val format: String,         // "MP3", "FLAC", etc.

    @ColumnInfo(name = "track_number")
    val trackNumber: Int?,

    val year: Int?,
    val genre: String?,

    @ColumnInfo(name = "last_scanned_at")
    val lastScannedAt: Long     // epoch ms
)
