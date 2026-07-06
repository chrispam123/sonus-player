package com.sonus.player.data.repository

import android.util.Log
import com.sonus.player.data.remote.jamendo.JamendoApiService
import com.sonus.player.data.remote.jamendo.JamendoTrack
import com.sonus.player.domain.model.AudioFormat
import com.sonus.player.domain.model.Track
import javax.inject.Inject

/**
 * Searches Jamendo for music and converts results to domain Track objects.
 * Tracks from Jamendo have streamUrl set (remote playback) and coverArtUrl.
 */
class JamendoRepositoryImpl @Inject constructor(
    private val jamendoApi: JamendoApiService
) {
    companion object {
        private const val TAG = "JamendoRepo"
        private val CLIENT_ID = com.sonus.player.data.BuildConfig.JAMENDO_CLIENT_ID
    }

    suspend fun searchTracks(query: String): List<Track> {
        if (CLIENT_ID.isEmpty()) {
            Log.d(TAG, "No Jamendo client ID configured")
            return emptyList()
        }

        return try {
            Log.d(TAG, "Searching Jamendo: '$query'")
            val response = jamendoApi.searchTracks(
                clientId = CLIENT_ID,
                query = query
            )

            Log.d(TAG, "Jamendo response: status=${response.headers?.status}, code=${response.headers?.code}, count=${response.headers?.results_count}")
            Log.d(TAG, "Jamendo results raw size: ${response.results?.size}")

            if (response.results != null && response.results.isNotEmpty()) {
                val firstResult = response.results.first()
                Log.d(TAG, "First result: id=${firstResult.id}, name=${firstResult.name}, artist=${firstResult.artist_name}, audio=${firstResult.audio}")
            }

            val tracks = response.results?.mapNotNull { it.toDomainTrack() } ?: emptyList()
            Log.d(TAG, "Jamendo found ${tracks.size} tracks")
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Jamendo search error: ${e.message}")
            emptyList()
        }
    }

    private fun JamendoTrack.toDomainTrack(): Track? {
        val streamUrl = audio ?: audiodownload ?: return null
        val trackId = id?.toLongOrNull() ?: return null

        return Track(
            id = trackId + 900_000_000L, // Offset to avoid collision with local track IDs
            title = name ?: "Unknown",
            artist = artist_name ?: "Unknown Artist",
            album = album_name ?: "Jamendo",
            albumId = trackId,
            duration = (duration ?: 0) * 1000L, // seconds to milliseconds
            filePath = "",
            fileSize = 0,
            bitrate = 320,
            sampleRate = 44100,
            format = AudioFormat.MP3,
            trackNumber = null,
            year = null,
            genre = null,
            streamUrl = streamUrl,
            coverArtUrl = album_image ?: image
        )
    }
}
