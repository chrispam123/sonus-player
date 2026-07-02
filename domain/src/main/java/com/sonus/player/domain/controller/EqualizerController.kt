package com.sonus.player.domain.controller

import com.sonus.player.domain.model.EqPreset
import kotlinx.coroutines.flow.StateFlow

interface EqualizerController {
    val currentPreset: StateFlow<EqPreset>
    val isEnabled: StateFlow<Boolean>
    fun applyPreset(preset: EqPreset)
    fun setEnabled(enabled: Boolean)
}
