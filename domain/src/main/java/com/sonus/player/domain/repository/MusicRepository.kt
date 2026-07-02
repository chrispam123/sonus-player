package com.sonus.player.domain.repository

import com.sonus.player.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllTracks(): Flow<List<Track>>
    fun getTrackById(id: Long): Flow<Track?>
    fun searchTracks(query: String): Flow<List<Track>>
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>
    fun getTracksByArtist(artistId: Long): Flow<List<Track>>
}
