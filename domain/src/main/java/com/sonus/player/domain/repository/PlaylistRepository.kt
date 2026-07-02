package com.sonus.player.domain.repository

import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistWithTracks(id: Long): Flow<PlaylistWithTracks?>
    suspend fun createPlaylist(name: String): Long
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    suspend fun deletePlaylist(id: Long)
}
