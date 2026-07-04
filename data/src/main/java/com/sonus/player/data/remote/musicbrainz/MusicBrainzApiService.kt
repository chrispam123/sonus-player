package com.sonus.player.data.remote.musicbrainz

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface MusicBrainzApiService {

    @Headers("User-Agent: Sonus/1.0 (music-player-app)")
    @GET("release/")
    suspend fun searchRelease(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 1
    ): MusicBrainzSearchResponse
}

data class MusicBrainzSearchResponse(
    val releases: List<MusicBrainzRelease>?
)

data class MusicBrainzRelease(
    val id: String,
    val title: String?
)
