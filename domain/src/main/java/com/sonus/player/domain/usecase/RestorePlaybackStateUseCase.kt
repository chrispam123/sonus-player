package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.SavedPlaybackState
import com.sonus.player.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class RestorePlaybackStateUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<SavedPlaybackState?> = repository.getLastPlaybackState()
}
