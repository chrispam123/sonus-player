package com.sonus.player.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.data.player.Media3PlayerController
import com.sonus.player.domain.controller.PlayerController
import com.sonus.player.domain.model.PlaybackProgress
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.domain.model.SavedPlaybackState
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import com.sonus.player.domain.repository.PlaybackHistoryRepository
import com.sonus.player.domain.repository.PreferencesRepository
import com.sonus.player.ui.visualizer.AudioVisualizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: PlaybackProgress = PlaybackProgress(0, 0, 0),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val historyRepository: PlaybackHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerVM"
        private const val SAVE_DEBOUNCE_MS = 5000L
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Error events (consumed once by UI via Snackbar)
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var lastTrackedTrackId: Long = -1
    private var saveJob: Job? = null

    // Audio Visualizer for Living Canvas
    private val audioVisualizer = AudioVisualizer()
    val fftData: StateFlow<FloatArray> = audioVisualizer.fftData
    val amplitude: StateFlow<Float> = audioVisualizer.amplitude

    init {
        observePlayback()
        connectToService()
    }

    private fun connectToService() {
        viewModelScope.launch {
            try {
                val controller = playerController as? Media3PlayerController
                controller?.connect()

                // Attach EQ — shorter delay, retry if needed
                delay(500)
                var sessionId = com.sonus.player.playback.PlaybackService.audioSessionId
                if (sessionId != 0) {
                    controller?.attachEqualizer(sessionId)
                    audioVisualizer.start(sessionId)
                } else {
                    // Retry once after another 500ms
                    delay(500)
                    sessionId = com.sonus.player.playback.PlaybackService.audioSessionId
                    if (sessionId != 0) {
                        controller?.attachEqualizer(sessionId)
                        audioVisualizer.start(sessionId)
                    }
                }

                // Restore last playback state
                restorePlaybackState()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to service: ${e.message}")
            }
        }
    }

    private var lastSavedTrackId: Long = -1
    private var lastSavedIsPlaying: Boolean = false

    private fun observePlayback() {
        viewModelScope.launch {
            playerController.playbackState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    currentTrack = state.currentTrack,
                    isPlaying = state.isPlaying,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    error = null
                )

                val track = state.currentTrack
                if (track != null && state.isPlaying && track.id != lastTrackedTrackId) {
                    lastTrackedTrackId = track.id
                    try {
                        historyRepository.addToHistory(track)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding to history: ${e.message}")
                    }

                    // Re-attach EQ
                    val sessionId = com.sonus.player.playback.PlaybackService.audioSessionId
                    if (sessionId != 0) {
                        (playerController as? Media3PlayerController)?.attachEqualizer(sessionId)
                    }
                }

                // Only save state when track changes or play/pause changes (not every 100ms)
                val trackChanged = track?.id != lastSavedTrackId
                val playChanged = state.isPlaying != lastSavedIsPlaying
                if (track != null && (trackChanged || playChanged)) {
                    lastSavedTrackId = track.id
                    lastSavedIsPlaying = state.isPlaying
                    debounceSaveState()
                }
            }
        }
        viewModelScope.launch {
            playerController.progress.collect { progress ->
                _uiState.value = _uiState.value.copy(progress = progress)
            }
        }
    }

    // =========================================================
    // PLAYBACK CONTROLS (with error handling)
    // =========================================================

    fun playTrack(track: Track, queue: List<Track>) {
        try {
            val index = queue.indexOf(track).coerceAtLeast(0)
            playerController.playQueue(queue, index)
        } catch (e: Exception) {
            handleError("No se pudo reproducir: ${track.title}")
        }
    }

    fun togglePlayPause() {
        try {
            val track = _uiState.value.currentTrack
            if (track == null) {
                Log.d(TAG, "togglePlayPause: no track loaded, ignoring")
                return
            }
            if (_uiState.value.isPlaying) {
                playerController.pause()
                saveStateNow()
            } else {
                playerController.resume()
            }
        } catch (e: Exception) {
            handleError("Error de reproducción")
        }
    }

    fun next() {
        try {
            playerController.next()
        } catch (e: Exception) {
            handleError("No se pudo avanzar a la siguiente pista")
        }
    }

    fun previous() {
        try {
            playerController.previous()
        } catch (e: Exception) {
            handleError("No se pudo retroceder")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            playerController.seekTo(positionMs)
        } catch (e: Exception) {
            // Silent — seeking errors are non-critical
        }
    }

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

    // =========================================================
    // PERSISTENCE — Save & Restore playback state
    // =========================================================

    private fun debounceSaveState() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveStateNow()
        }
    }

    private fun saveStateNow() {
        val track = _uiState.value.currentTrack ?: return
        val progress = _uiState.value.progress

        viewModelScope.launch {
            try {
                val state = SavedPlaybackState(
                    trackId = track.id,
                    positionMs = progress.positionMs,
                    queueTrackIds = playerController.queue.value.map { it.id },
                    queueIndex = playerController.queue.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0),
                    shuffleEnabled = _uiState.value.shuffleEnabled,
                    repeatMode = _uiState.value.repeatMode
                )
                preferencesRepository.savePlaybackState(state)
                Log.d(TAG, "State saved: track=${track.title}, pos=${progress.positionMs}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving state: ${e.message}")
            }
        }
    }

    private fun restorePlaybackState() {
        viewModelScope.launch {
            try {
                val saved = preferencesRepository.getLastPlaybackState().firstOrNull() ?: return@launch
                Log.d(TAG, "Restoring state: trackId=${saved.trackId}, pos=${saved.positionMs}")

                // Find the track in the database
                val track = musicRepository.getTrackById(saved.trackId).firstOrNull() ?: return@launch

                // Load the track into the player (prepared but not playing)
                // This ensures that when user taps Play, it works immediately
                playerController.play(track)
                // Immediately pause — we just want it loaded, not playing
                delay(300)
                playerController.pause()
                if (saved.positionMs > 0) {
                    playerController.seekTo(saved.positionMs)
                }

                Log.d(TAG, "State restored: ${track.title} at ${saved.positionMs}ms (paused)")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring state: ${e.message}")
            }
        }
    }

    // =========================================================
    // ERROR HANDLING
    // =========================================================

    private fun handleError(message: String) {
        Log.e(TAG, message)
        viewModelScope.launch {
            _errorEvents.emit(message)
        }
    }

    /**
     * Called when app goes to background (from Activity lifecycle)
     */
    fun onAppBackground() {
        saveStateNow()
    }

    override fun onCleared() {
        saveStateNow()
        audioVisualizer.release()
        super.onCleared()
    }
}
