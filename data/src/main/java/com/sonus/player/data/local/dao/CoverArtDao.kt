package com.sonus.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonus.player.data.local.entity.CoverArtCacheEntity

@Dao
interface CoverArtDao {

    @Query("SELECT * FROM cover_art_cache WHERE album_id = :albumId")
    suspend fun getByAlbumId(albumId: Long): CoverArtCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoverArtCacheEntity)

    @Query("DELETE FROM cover_art_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cover_art_cache")
    suspend fun getCount(): Int
}
