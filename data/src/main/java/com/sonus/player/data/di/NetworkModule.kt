package com.sonus.player.data.di

import com.sonus.player.data.remote.lastfm.LastFmApiService
import com.sonus.player.data.remote.musicbrainz.MusicBrainzApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("lastfm")
    fun provideLastFmRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("musicbrainz")
    fun provideMusicBrainzRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLastFmApiService(@Named("lastfm") retrofit: Retrofit): LastFmApiService =
        retrofit.create(LastFmApiService::class.java)

    @Provides
    @Singleton
    fun provideMusicBrainzApiService(@Named("musicbrainz") retrofit: Retrofit): MusicBrainzApiService =
        retrofit.create(MusicBrainzApiService::class.java)
}
