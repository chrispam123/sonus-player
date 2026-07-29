package com.sonus.player.data.mapper

import com.sonus.player.data.local.entity.TrackEntity
import com.sonus.player.domain.model.AudioFormat
import com.sonus.player.domain.model.Track

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    duration = duration,
    filePath = filePath,
    fileSize = fileSize,
    bitrate = bitrate,
    sampleRate = sampleRate,
    format = parseAudioFormat(format),
    trackNumber = trackNumber,
    year = year,
    genre = genre,
    streamUrl = streamUrl,  // 🆕 Recuperar URL de streaming
    coverArtUrl = null
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    duration = duration,
    filePath = filePath,
    fileSize = fileSize,
    bitrate = bitrate,
    sampleRate = sampleRate,
    format = format.name,
    trackNumber = trackNumber,
    year = year,
    genre = genre,
    streamUrl = streamUrl,  // 🆕 Persistir URL de streaming
    lastScannedAt = System.currentTimeMillis()
)

private fun parseAudioFormat(format: String): AudioFormat = when (format.uppercase()) {
    "MP3" -> AudioFormat.MP3
    "AAC", "M4A" -> AudioFormat.AAC
    "FLAC" -> AudioFormat.FLAC
    "OGG" -> AudioFormat.OGG
    "WAV" -> AudioFormat.WAV
    else -> AudioFormat.MP3
}
