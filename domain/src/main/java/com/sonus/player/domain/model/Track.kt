package com.sonus.player.domain.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val filePath: String,
    val fileSize: Long,
    val bitrate: Int,
    val sampleRate: Int,
    val format: AudioFormat,
    val trackNumber: Int?,
    val year: Int?,
    val genre: String?
)
