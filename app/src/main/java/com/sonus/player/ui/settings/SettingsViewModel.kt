package com.sonus.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.domain.controller.EqualizerController
import com.sonus.player.domain.controller.PlayerController
import com.sonus.player.domain.model.EqPreset
import com.sonus.player.domain.model.ThemeMode
import com.sonus.player.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqEnabled: Boolean = false,
    val sleepTimerRemainingMs: Long? = null,
    val sleepTimerActive: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val equalizerController: EqualizerController,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        observePreferences()
        observeEq()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.getThemeMode().collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    private fun observeEq() {
        viewModelScope.launch {
            equalizerController.currentPreset.collect { preset ->
                _uiState.value = _uiState.value.copy(eqPreset = preset)
            }
        }
        viewModelScope.launch {
            equalizerController.isEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(eqEnabled = enabled)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setEqPreset(preset: EqPreset) {
        equalizerController.applyPreset(preset)
        viewModelScope.launch {
            preferencesRepository.setActiveEqPreset(preset)
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val durationMs = minutes * 60_000L

        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            _uiState.value = _uiState.value.copy(
                sleepTimerRemainingMs = remaining,
                sleepTimerActive = true
            )

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining -= 1000
                _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = remaining)
            }

            if (isActive) {
                // Timer expired — pause playback
                playerController.pause()
                _uiState.value = _uiState.value.copy(
                    sleepTimerRemainingMs = null,
                    sleepTimerActive = false
                )
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingMs = null,
            sleepTimerActive = false
        )
    }
}
