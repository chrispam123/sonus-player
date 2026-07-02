package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.SavedPlaybackState
import com.sonus.player.domain.repository.PreferencesRepository

class SavePlaybackStateUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(state: SavedPlaybackState) = repository.savePlaybackState(state)
}
