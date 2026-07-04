package com.sonus.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sonus.player.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("""
        SELECT * FROM playback_history 
        ORDER BY played_at DESC 
        LIMIT :limit
    """)
    fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getCount(): Int

    // Delete oldest entries beyond the limit
    @Query("""
        DELETE FROM playback_history 
        WHERE id NOT IN (
            SELECT id FROM playback_history 
            ORDER BY played_at DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun trimToLimit(keepCount: Int)
}
