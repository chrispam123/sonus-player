package com.sonus.player.di

import com.sonus.player.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

// ============================================================
// APP MODULE — Proveedores que dependen de BuildConfig
// ============================================================
// BuildConfig solo existe en el módulo :app (productFlavors).
// Este módulo Hilt inyecta los valores en :data sin dependencia circular.
// ============================================================

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named("sonus_base_url")
    fun provideSonusBaseUrl(): String = BuildConfig.SONUS_API_URL
}
