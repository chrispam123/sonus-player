package com.sonus.player.data.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.sonus.player.data.local.SonusDatabase
import com.sonus.player.data.local.dao.CoverArtDao
import com.sonus.player.data.local.dao.LyricsDao
import com.sonus.player.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSonusDatabase(@ApplicationContext context: Context): SonusDatabase =
        Room.databaseBuilder(
            context,
            SonusDatabase::class.java,
            "sonus_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()

    @Provides
    fun provideTrackDao(database: SonusDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideCoverArtDao(database: SonusDatabase): CoverArtDao = database.coverArtDao()

    @Provides
    fun provideLyricsDao(database: SonusDatabase): LyricsDao = database.lyricsDao()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
