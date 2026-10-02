package com.sonus.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonus.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query(
        """
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%'
        OR artist LIKE '%' || :query || '%'
        OR album LIKE '%' || :query || '%'
        ORDER BY title ASC
    """
    )
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album_id = :albumId ORDER BY track_number ASC")
    fun getTracksByAlbum(albumId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY album, track_number ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    // IGNORE (no REPLACE): REPLACE hace DELETE+INSERT internamente,
    // lo que dispara el ON DELETE CASCADE de playlist_tracks y borra
    // las canciones de las listas en cada re-escaneo. IGNORE salta los
    // tracks existentes (sin borrar) y solo inserta los nuevos.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    // 🆕 Borra pistas LOCALES fantasma (archivo borrado/renombrado del dispositivo)
    // que ya no aparecen en MediaStore. NO toca las de ccMixter/streaming:
    // stream_url IS NULL las excluye (las de streaming tienen stream_url != NULL).
    @Query("DELETE FROM tracks WHERE stream_url IS NULL AND id NOT IN (:validIds)")
    suspend fun deleteMissingLocalTracks(validIds: List<Long>)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}
