package com.sonus.player.data.di

import com.sonus.player.data.player.Media3PlayerController
import com.sonus.player.domain.controller.PlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: Media3PlayerController): PlayerController
}
