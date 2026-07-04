package com.sonus.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sonus.player.data.local.dao.CoverArtDao
import com.sonus.player.data.local.dao.LyricsDao
import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.CoverArtCacheEntity
import com.sonus.player.data.local.entity.LyricsCacheEntity
import com.sonus.player.data.local.entity.TrackEntity

@Database(
    entities = [TrackEntity::class, CoverArtCacheEntity::class, LyricsCacheEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SonusDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun coverArtDao(): CoverArtDao
    abstract fun lyricsDao(): LyricsDao
}
