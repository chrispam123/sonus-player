package com.sonus.player.data.remote.lastfm

import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApiService {

    @GET("2.0/")
    suspend fun getAlbumInfo(
        @Query("method") method: String = "album.getinfo",
        @Query("artist") artist: String,
        @Query("album") album: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): LastFmAlbumResponse
}

data class LastFmAlbumResponse(
    val album: LastFmAlbum?
)

data class LastFmAlbum(
    val image: List<LastFmImage>?
)

data class LastFmImage(
    val size: String,       // "small", "medium", "large", "extralarge", "mega"
    @com.google.gson.annotations.SerializedName("#text")
    val url: String
)
