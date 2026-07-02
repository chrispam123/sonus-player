package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.PlaylistWithTracks
import com.sonus.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class ManagePlaylistUseCase(private val repository: PlaylistRepository) {

    fun getAll(): Flow<List<Playlist>> = repository.getAllPlaylists()

    fun getWithTracks(id: Long): Flow<PlaylistWithTracks?> = repository.getPlaylistWithTracks(id)

    suspend fun create(name: String): Long = repository.createPlaylist(name)

    suspend fun addTrack(playlistId: Long, trackId: Long) =
        repository.addTrackToPlaylist(playlistId, trackId)

    suspend fun removeTrack(playlistId: Long, trackId: Long) =
        repository.removeTrackFromPlaylist(playlistId, trackId)

    suspend fun delete(id: Long) = repository.deletePlaylist(id)
}
