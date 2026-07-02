package com.sonus.player.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int
)

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<Track>
)
