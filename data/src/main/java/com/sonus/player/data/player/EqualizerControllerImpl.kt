package com.sonus.player.data.player

import android.media.audiofx.Equalizer
import android.util.Log
import com.sonus.player.domain.controller.EqualizerController
import com.sonus.player.domain.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerControllerImpl @Inject constructor() : EqualizerController {

    companion object {
        private const val TAG = "EqualizerCtrl"
    }

    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = -1

    private val _currentPreset = MutableStateFlow(EqPreset.FLAT)
    override val currentPreset: StateFlow<EqPreset> = _currentPreset.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    /**
     * Attach equalizer to an audio session.
     * Must be called with a valid audioSessionId from ExoPlayer.
     */
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return

        Log.d(TAG, "Attaching EQ to session ID: $audioSessionId")
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
            }
            currentSessionId = audioSessionId

            val numBands = equalizer!!.numberOfBands
            val range = equalizer!!.bandLevelRange
            Log.d(TAG, "EQ attached: $numBands bands, range ${range[0]}..${range[1]} millibels")

            // Apply current preset if enabled
            if (_isEnabled.value) {
                applyBandsToEqualizer(_currentPreset.value.bands)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach EQ: ${e.message}")
            equalizer = null
        }
    }

    override fun applyPreset(preset: EqPreset) {
        Log.d(TAG, "Applying preset: ${preset.name}, enabled=${_isEnabled.value}")
        _currentPreset.value = preset
        if (_isEnabled.value) {
            applyBandsToEqualizer(preset.bands)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        Log.d(TAG, "EQ enabled: $enabled, session=$currentSessionId")
        _isEnabled.value = enabled
        equalizer?.enabled = enabled
        if (enabled) {
            applyBandsToEqualizer(_currentPreset.value.bands)
        }
    }

    private fun applyBandsToEqualizer(bands: List<Float>) {
        val eq = equalizer
        if (eq == null) {
            Log.w(TAG, "EQ not attached, cannot apply bands")
            return
        }

        val numBands = eq.numberOfBands.toInt()
        val minLevel = eq.bandLevelRange[0] // typically -1500 (millibels)
        val maxLevel = eq.bandLevelRange[1] // typically +1500

        for (i in 0 until minOf(bands.size, numBands)) {
            // Convert dB (-12 to +12) to millibels (-1200 to +1200)
            val millibels = (bands[i] * 100).toInt().toShort()
            val clamped = millibels.coerceIn(minLevel, maxLevel)
            eq.setBandLevel(i.toShort(), clamped)
            Log.d(TAG, "Band $i: ${bands[i]}dB → ${clamped}mb")
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        currentSessionId = -1
    }
}
