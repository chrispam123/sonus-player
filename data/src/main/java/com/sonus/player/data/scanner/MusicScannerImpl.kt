package com.sonus.player.data.scanner

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import com.sonus.player.data.local.entity.TrackEntity
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicScannerRepository
import com.sonus.player.domain.repository.ScanProgress
import com.sonus.player.data.mapper.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MusicScannerImpl @Inject constructor(
    private val contentResolver: ContentResolver
) : MusicScannerRepository {

    private val _scanProgress = MutableStateFlow(
        ScanProgress(totalFiles = 0, processedFiles = 0, isScanning = false)
    )

    override fun getScanProgress(): Flow<ScanProgress> = _scanProgress

    override suspend fun scanLibrary(): List<Track> = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgress(0, 0, isScanning = true)

        val tracks = mutableListOf<TrackEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        // Only music files, not ringtones/notifications, minimum 30 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use { c ->
            val totalFiles = c.count
            _scanProgress.value = ScanProgress(totalFiles, 0, isScanning = true)

            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            var processed = 0
            while (c.moveToNext()) {
                try {
                    val entity = TrackEntity(
                        id = c.getLong(idCol),
                        title = c.getString(titleCol) ?: "Unknown",
                        artist = c.getString(artistCol) ?: "Unknown Artist",
                        album = c.getString(albumCol) ?: "Unknown Album",
                        albumId = c.getLong(albumIdCol),
                        duration = c.getLong(durationCol),
                        filePath = c.getString(dataCol) ?: "",
                        fileSize = c.getLong(sizeCol),
                        bitrate = 0, // MediaStore doesn't provide this directly
                        sampleRate = 0,
                        format = mimeTypeToFormat(c.getString(mimeCol) ?: ""),
                        trackNumber = c.getInt(trackCol).takeIf { it > 0 },
                        year = c.getInt(yearCol).takeIf { it > 0 },
                        genre = null,
                        lastScannedAt = System.currentTimeMillis()
                    )
                    tracks.add(entity)
                } catch (e: Exception) {
                    // Skip corrupt/unreadable files, continue scanning
                }
                processed++
                _scanProgress.value = ScanProgress(totalFiles, processed, isScanning = true)
            }
        }

        _scanProgress.value = ScanProgress(tracks.size, tracks.size, isScanning = false)
        tracks.map { it.toDomain() }
    }

    private fun mimeTypeToFormat(mimeType: String): String = when {
        mimeType.contains("mp3") || mimeType.contains("mpeg") -> "MP3"
        mimeType.contains("aac") || mimeType.contains("mp4") -> "AAC"
        mimeType.contains("flac") -> "FLAC"
        mimeType.contains("ogg") -> "OGG"
        mimeType.contains("wav") -> "WAV"
        else -> "MP3"
    }
}
