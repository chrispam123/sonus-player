package com.sonus.player.data.di

import com.sonus.player.data.preferences.PreferencesRepositoryImpl
import com.sonus.player.data.repository.CoverArtResolverImpl
import com.sonus.player.data.repository.LyricsRepositoryImpl
import com.sonus.player.data.repository.MusicRepositoryImpl
import com.sonus.player.data.repository.PlaybackHistoryRepositoryImpl
import com.sonus.player.data.repository.PlaylistRepositoryImpl
import com.sonus.player.data.scanner.MusicScannerImpl
import com.sonus.player.domain.repository.CoverArtRepository
import com.sonus.player.domain.repository.LyricsRepository
import com.sonus.player.domain.repository.MusicRepository
import com.sonus.player.domain.repository.MusicScannerRepository
import com.sonus.player.domain.repository.PlaybackHistoryRepository
import com.sonus.player.domain.repository.PlaylistRepository
import com.sonus.player.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindMusicScanner(impl: MusicScannerImpl): MusicScannerRepository

    @Binds
    @Singleton
    abstract fun bindCoverArtRepository(impl: CoverArtResolverImpl): CoverArtRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackHistoryRepository(impl: PlaybackHistoryRepositoryImpl): PlaybackHistoryRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
