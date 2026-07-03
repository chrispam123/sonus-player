package com.sonus.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.TrackEntity

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SonusDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
