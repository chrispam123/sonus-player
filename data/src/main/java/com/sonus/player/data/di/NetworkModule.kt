package com.sonus.player.data.di

import com.sonus.player.data.remote.ccmixter.CcMixterApiService
import com.sonus.player.data.remote.genius.GeniusApiService
import com.sonus.player.data.remote.lastfm.LastFmApiService
import com.sonus.player.data.remote.lrclib.LrcLibApiService
import com.sonus.player.data.remote.musicbrainz.MusicBrainzApiService
import com.sonus.player.data.remote.sonus.SonusApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.Response
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
            // 🆕 Forzar HTTP/1.1: LRCLIB (detrás de Cloudflare) devuelve 520
            // con el HTTP/2 de OkHttp. curl y Chrome (otras pilas) sí funcionan.
            // HTTP/1.1 evita el problema y es compatible con todas nuestras APIs.
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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

    @Provides
    @Singleton
    @Named("lrclib")
    fun provideLrcLibRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLrcLibApiService(@Named("lrclib") retrofit: Retrofit): LrcLibApiService =
        retrofit.create(LrcLibApiService::class.java)

    @Provides
    @Singleton
    @Named("genius")
    fun provideGeniusRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.genius.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGeniusApiService(@Named("genius") retrofit: Retrofit): GeniusApiService =
        retrofit.create(GeniusApiService::class.java)

    @Provides
    @Singleton
    @Named("jamendo")
    fun provideJamendoRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://ccmixter.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideCcMixterApiService(@Named("jamendo") retrofit: Retrofit): CcMixterApiService =
        retrofit.create(CcMixterApiService::class.java)

    // ── SONUS BACKEND ──────────────────────────────────────
    // 🆕 OkHttpClient específico para Sonus: añade x-api-key a cada request.
    // El interceptor lee la key del BuildConfig inyectado vía Hilt.
    // ============================================================

    @Provides
    @Singleton
    @Named("sonus")
    fun provideSonusOkHttpClient(
        baseClient: OkHttpClient,
        @Named("sonus_api_key") apiKey: String
    ): OkHttpClient =
        baseClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-api-key", apiKey)
                    .build()
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    @Named("sonus")
    fun provideSonusRetrofit(
        @Named("sonus") client: OkHttpClient,
        @Named("sonus_base_url") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSonusApiService(@Named("sonus") retrofit: Retrofit): SonusApiService =
        retrofit.create(SonusApiService::class.java)
}
