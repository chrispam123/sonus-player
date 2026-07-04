package com.sonus.player.data.remote.lrclib

import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApiService {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String,
        @Query("duration") duration: Int? = null
    ): LrcLibResponse
}

data class LrcLibResponse(
    val id: Int?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Int?,
    val plainLyrics: String?,
    val syncedLyrics: String?       // LRC format with timestamps
)
