package com.sonus.player.data.repository

import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.TrackEntity
import com.sonus.player.data.mapper.toDomain
import com.sonus.player.data.mapper.toEntity
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao
) : MusicRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTrackById(id: Long): Flow<Track?> =
        kotlinx.coroutines.flow.flow {
            emit(trackDao.getTrackById(id)?.toDomain())
        }

    override fun searchTracks(query: String): Flow<List<Track>> =
        trackDao.searchTracks(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTracksByAlbum(albumId: Long): Flow<List<Track>> =
        trackDao.getTracksByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTracksByArtist(artistId: Long): Flow<List<Track>> {
        // For now, we use artistId as a placeholder.
        // In the full implementation, we'd resolve artist name from ID.
        return trackDao.getAllTracks().map { entities ->
            entities.filter { it.albumId == artistId }.map { it.toDomain() }
        }
    }

    suspend fun indexScannedTracks(tracks: List<Track>) {
        val entities = tracks.map { it.toEntity() }
        trackDao.insertAll(entities)
    }
}
