package com.sonus.player.domain.repository

import com.sonus.player.domain.model.EqPreset
import com.sonus.player.domain.model.SavedPlaybackState
import com.sonus.player.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun getLastPlaybackState(): Flow<SavedPlaybackState?>
    suspend fun savePlaybackState(state: SavedPlaybackState)
    fun getActiveEqPreset(): Flow<EqPreset>
    suspend fun setActiveEqPreset(preset: EqPreset)
}
