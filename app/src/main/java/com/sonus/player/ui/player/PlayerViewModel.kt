package com.sonus.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.data.player.Media3PlayerController
import com.sonus.player.domain.controller.PlayerController
import com.sonus.player.domain.model.PlaybackProgress
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.PlaybackHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: PlaybackProgress = PlaybackProgress(0, 0, 0),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val historyRepository: PlaybackHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var lastTrackedTrackId: Long = -1

    init {
        observePlayback()
        connectToService()
    }

    private fun connectToService() {
        viewModelScope.launch {
            val controller = playerController as? Media3PlayerController
            controller?.connect()
            // Try attaching EQ after service starts
            kotlinx.coroutines.delay(2000)
            val sessionId = com.sonus.player.playback.PlaybackService.audioSessionId
            if (sessionId != 0) {
                controller?.attachEqualizer(sessionId)
            }
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            playerController.playbackState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    currentTrack = state.currentTrack,
                    isPlaying = state.isPlaying,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode
                )
                // Track history when a new song starts playing
                val track = state.currentTrack
                if (track != null && state.isPlaying && track.id != lastTrackedTrackId) {
                    lastTrackedTrackId = track.id
                    historyRepository.addToHistory(track)

                    // Re-attach EQ when playback starts (ensures session ID is valid)
                    val sessionId = com.sonus.player.playback.PlaybackService.audioSessionId
                    if (sessionId != 0) {
                        (playerController as? Media3PlayerController)?.attachEqualizer(sessionId)
                    }
                }
            }
        }
        viewModelScope.launch {
            playerController.progress.collect { progress ->
                _uiState.value = _uiState.value.copy(progress = progress)
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        val index = queue.indexOf(track).coerceAtLeast(0)
        playerController.playQueue(queue, index)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            playerController.pause()
        } else {
            playerController.resume()
        }
    }

    fun next() = playerController.next()
    fun previous() = playerController.previous()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleShuffle() {
        playerController.setShuffleEnabled(!_uiState.value.shuffleEnabled)
    }

    fun toggleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerController.setRepeatMode(next)
    }
}
