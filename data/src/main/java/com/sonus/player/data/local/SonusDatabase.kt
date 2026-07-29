package com.sonus.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sonus.player.data.local.dao.CoverArtDao
import com.sonus.player.data.local.dao.HistoryDao
import com.sonus.player.data.local.dao.LyricsDao
import com.sonus.player.data.local.dao.PlaylistDao
import com.sonus.player.data.local.dao.TrackDao
import com.sonus.player.data.local.entity.CoverArtCacheEntity
import com.sonus.player.data.local.entity.HistoryEntity
import com.sonus.player.data.local.entity.LyricsCacheEntity
import com.sonus.player.data.local.entity.PlaylistEntity
import com.sonus.player.data.local.entity.PlaylistTrackCrossRef
import com.sonus.player.data.local.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        CoverArtCacheEntity::class,
        LyricsCacheEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        HistoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SonusDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun coverArtDao(): CoverArtDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
}
