package com.sonus.player.data.repository

import android.util.Log
import com.sonus.player.data.remote.ccmixter.CcMixterApiService
import com.sonus.player.data.remote.ccmixter.CcMixterTrack
import com.sonus.player.domain.model.AudioFormat
import com.sonus.player.domain.model.Track
import javax.inject.Inject

/**
 * Searches ccMixter for Creative Commons music and converts results to domain Track objects.
 * Tracks from ccMixter have streamUrl set (remote playback).
 * No API key required.
 */
class StreamingRepositoryImpl @Inject constructor(
    private val ccMixterApi: CcMixterApiService
) {
    companion object {
        private const val TAG = "StreamingRepo"
    }

    suspend fun searchTracks(query: String): List<Track> {
        return try {
            Log.d(TAG, "Searching ccMixter: '$query'")
            val results = ccMixterApi.searchByTags(tags = query)
            Log.d(TAG, "ccMixter returned ${results.size} results")

            val tracks = results.mapNotNull { it.toDomainTrack() }
            Log.d(TAG, "Converted to ${tracks.size} playable tracks")
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "ccMixter search error: ${e.message}")
            emptyList()
        }
    }

    private fun CcMixterTrack.toDomainTrack(): Track? {
        val id = upload_id ?: return null
        val title = upload_name ?: return null

        // Find the MP3 file (prefer mp3 nicname)
        val mp3File = files?.firstOrNull { it.file_nicname == "mp3" }
            ?: files?.firstOrNull { it.download_url?.endsWith(".mp3") == true }
            ?: files?.firstOrNull()
            ?: return null

        val streamUrl = mp3File.download_url ?: return null
        val durationMs = parseDuration(mp3File.file_format_info?.ps)

        return Track(
            id = id + 800_000_000L, // Offset to avoid collision with local IDs
            title = title,
            artist = user_real_name ?: user_name ?: "Unknown Artist",
            album = "ccMixter",
            albumId = id,
            duration = durationMs,
            filePath = "",
            fileSize = mp3File.file_rawsize ?: 0,
            bitrate = 320,
            sampleRate = parseSampleRate(mp3File.file_format_info?.sr),
            format = AudioFormat.MP3,
            trackNumber = null,
            year = null,
            genre = null,
            streamUrl = streamUrl,
            coverArtUrl = null // ccMixter doesn't provide cover art
        )
    }

    /**
     * Parses duration string like "1:40" or "3:22" to milliseconds
     */
    private fun parseDuration(ps: String?): Long {
        if (ps == null) return 0L
        val parts = ps.split(":")
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: 0
                val seconds = parts[1].toLongOrNull() ?: 0
                (minutes * 60 + seconds) * 1000
            }
            3 -> {
                val hours = parts[0].toLongOrNull() ?: 0
                val minutes = parts[1].toLongOrNull() ?: 0
                val seconds = parts[2].toLongOrNull() ?: 0
                (hours * 3600 + minutes * 60 + seconds) * 1000
            }
            else -> 0L
        }
    }

    private fun parseSampleRate(sr: String?): Int {
        if (sr == null) return 44100
        return when {
            sr.contains("48") -> 48000
            sr.contains("96") -> 96000
            sr.contains("24") -> 24000
            else -> 44100
        }
    }
}
