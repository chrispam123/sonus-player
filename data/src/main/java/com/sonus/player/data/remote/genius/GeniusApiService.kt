package com.sonus.player.data.remote.genius

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GeniusApiService {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Header("Authorization") token: String
    ): GeniusSearchResponse
}

data class GeniusSearchResponse(
    val response: GeniusResponseBody?
)

data class GeniusResponseBody(
    val hits: List<GeniusHit>?
)

data class GeniusHit(
    val result: GeniusResult?
)

data class GeniusResult(
    val id: Long?,
    val title: String?,
    val url: String?,
    val primary_artist: GeniusArtist?
)

data class GeniusArtist(
    val name: String?
)
