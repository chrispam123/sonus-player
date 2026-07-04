package com.sonus.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonus.player.data.local.entity.LyricsCacheEntity

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics_cache WHERE track_id = :trackId")
    suspend fun getByTrackId(trackId: Long): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAll()
}
