package com.sonus.player.data.remote.jamendo

import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {

    @GET("v3.0/tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("search") query: String,
        @Query("limit") limit: Int = 20,
        @Query("audioformat") audioFormat: String = "mp3"
    ): JamendoSearchResponse
}

data class JamendoSearchResponse(
    val headers: JamendoHeaders?,
    val results: List<JamendoTrack>?
)

data class JamendoHeaders(
    val status: String?,
    val code: Int?,
    val results_count: Int?
)

data class JamendoTrack(
    val id: String?,
    val name: String?,
    val artist_name: String?,
    val album_name: String?,
    val duration: Int?,          // seconds
    val audio: String?,          // MP3 stream URL
    val audiodownload: String?,  // MP3 download URL
    val image: String?,          // Cover art URL
    val album_image: String?     // Album cover URL
)
