package com.sonus.player.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonus.player.domain.model.EqPreset
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.domain.model.SavedPlaybackState
import com.sonus.player.domain.model.ThemeMode
import com.sonus.player.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonus_prefs")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
    }

    override fun getThemeMode(): Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[Keys.THEME_MODE]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    override fun getLastPlaybackState(): Flow<SavedPlaybackState?> =
        context.dataStore.data.map { prefs ->
            val trackId = prefs[Keys.LAST_TRACK_ID] ?: return@map null
            SavedPlaybackState(
                trackId = trackId,
                positionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
                queueTrackIds = emptyList(), // Simplified for now
                queueIndex = prefs[Keys.LAST_QUEUE_INDEX] ?: 0,
                shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
                repeatMode = when (prefs[Keys.REPEAT_MODE]) {
                    "one" -> RepeatMode.ONE
                    "all" -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            )
        }

    override suspend fun savePlaybackState(state: SavedPlaybackState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_TRACK_ID] = state.trackId
            prefs[Keys.LAST_POSITION_MS] = state.positionMs
            prefs[Keys.LAST_QUEUE_INDEX] = state.queueIndex
            prefs[Keys.SHUFFLE_ENABLED] = state.shuffleEnabled
            prefs[Keys.REPEAT_MODE] = when (state.repeatMode) {
                RepeatMode.OFF -> "off"
                RepeatMode.ONE -> "one"
                RepeatMode.ALL -> "all"
            }
        }
    }

    override fun getActiveEqPreset(): Flow<EqPreset> =
        context.dataStore.data.map { prefs ->
            val name = prefs[Keys.EQ_PRESET_NAME] ?: "Flat"
            EqPreset.ALL.find { it.name == name } ?: EqPreset.FLAT
        }

    override suspend fun setActiveEqPreset(preset: EqPreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_PRESET_NAME] = preset.name
        }
    }
}
