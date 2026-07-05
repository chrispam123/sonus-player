package com.sonus.player.data.repository

import com.sonus.player.data.local.dao.PlaylistDao
import com.sonus.player.data.local.dao.PlaylistWithCount
import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.PlaylistEntity
import com.sonus.player.data.local.entity.PlaylistTrackCrossRef
import com.sonus.player.data.mapper.toDomain
import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.PlaylistWithTracks
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylistsWithCount().map { entries ->
            entries.map { entry ->
                Playlist(
                    id = entry.id,
                    name = entry.name,
                    createdAt = entry.created_at,
                    trackCount = entry.track_count
                )
            }
        }

    override fun getPlaylistWithTracks(id: Long): Flow<PlaylistWithTracks?> =
        combine(
            playlistDao.getAllPlaylists(),
            playlistDao.getPlaylistTracks(id)
        ) { playlists, trackEntities ->
            val playlistEntity = playlists.find { it.id == id } ?: return@combine null
            val tracks = trackEntities.map { it.toDomain() }
            val count = tracks.size
            PlaylistWithTracks(
                playlist = Playlist(
                    id = playlistEntity.id,
                    name = playlistEntity.name,
                    createdAt = playlistEntity.createdAt,
                    trackCount = count
                ),
                tracks = tracks
            )
        }

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.createPlaylist(
            PlaylistEntity(name = name, createdAt = System.currentTimeMillis())
        )

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPos + 1,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)

    override suspend fun deletePlaylist(id: Long) =
        playlistDao.deletePlaylist(id)
}
