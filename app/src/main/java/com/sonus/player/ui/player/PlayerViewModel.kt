package com.sonus.player.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.data.player.Media3PlayerController
import com.sonus.player.data.repository.BackendRepositoryImpl
import com.sonus.player.data.remote.sonus.MoodRequestDto
import com.sonus.player.data.remote.sonus.TrackSummaryDto
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
    val error: String? = null,
    // 🆕 Análisis de mood con IA (backend Sonus + DeepSeek)
    val moodDescription: String? = null,      // "Pareces estar en un momento..."
    val moodLabel: String? = null,             // "melancholy" | "energetic" | ...
    val moodShaderSuggestion: com.sonus.player.ui.visualizer.ShaderRenderer.Mood? = null,
    val moodIsAnalyzing: Boolean = false,       // ¿Esperando respuesta del backend?
    val moodYoutubeLinks: List<String> = emptyList(),  // 🆕 Links sugeridos
    val moodViewed: Boolean = false  // 🆕 🔮 ya visto. Se resetea si el mood cambia.
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val historyRepository: PlaybackHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val musicRepository: MusicRepository,
    // 🆕 Backend Sonus: análisis de mood con DeepSeek IA
    private val backendRepository: BackendRepositoryImpl
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerVM"
        private const val SAVE_DEBOUNCE_MS = 5000L

        // 🆕 Timer de análisis de mood: cada 5 minutos.
        private const val MOOD_CHECK_INTERVAL_MS = 5 * 60 * 1000L
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Error events (consumed once by UI via Snackbar)
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var lastTrackedTrackId: Long = -1
    private var saveJob: Job? = null

    // 🆕 Último mood mostrado al usuario. Si el nuevo análisis coincide,
    // no se muestra el 🔮 (evita repetir el mismo mood).
    private var lastShownMoodLabel: String? = null

    // Audio Visualizer for Living Canvas
    private val audioVisualizer = AudioVisualizer()
    val fftData: StateFlow<FloatArray> = audioVisualizer.fftData
    val amplitude: StateFlow<Float> = audioVisualizer.amplitude

    init {
        observePlayback()
        connectToService()
        // 🆕 Iniciar timer de análisis de mood cada 30 minutos.
        // Si han pasado ≥60 min desde el último análisis, envía las
        // canciones recientes a DeepSeek para evaluar el estado de ánimo.
        startMoodAnalysisTimer()
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
    // 🆕 MOOD ANALYSIS — Análisis de estado de ánimo con IA
    // =========================================================
    // Cada 30 minutos, si hay ≥3 canciones en la última hora,
    // las envía al backend. DeepSeek analiza el patrón y devuelve
    // mood + sugerencia de shader + descripción.
    // =========================================================

    private fun startMoodAnalysisTimer() {
        viewModelScope.launch {
            while (true) {
                delay(MOOD_CHECK_INTERVAL_MS)
                requestMoodAnalysis()
            }
        }
    }

    /**
     * Envía el historial de la última hora al backend para
     * análisis de estado de ánimo con DeepSeek IA.
     * Se puede llamar manualmente para forzar un análisis.
     */
    private fun requestMoodAnalysis() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando análisis de mood...")

                // 1. Obtener historial reciente de Room
                val recentHistory = historyRepository.getRecentHistory(50)
                val historyList = recentHistory.first()
                val oneHourAgo = System.currentTimeMillis() - 3600_000
                val hourTracks = historyList.filter { it.playedAt > oneHourAgo }

                if (hourTracks.size < 3) {
                    Log.d(TAG, "Mood: insuficientes canciones (${hourTracks.size}), se necesitan ≥3")
                    return@launch
                }

                Log.d(TAG, "Mood: enviando ${hourTracks.size} canciones al backend")

                // 2. Convertir historial a DTO del backend
                val request = MoodRequestDto(
                    userId = "user_1",
                    tracks = hourTracks.map { entry ->
                        TrackSummaryDto(
                            title = entry.track.title,
                            artist = entry.track.artist,
                            durationMs = entry.track.duration,
                            playedAt = entry.playedAt
                        )
                    },
                    periodStart = oneHourAgo,
                    periodEnd = System.currentTimeMillis()
                )

                // 3. Enviar al backend (el Repository hace polling automático)
                _uiState.value = _uiState.value.copy(moodIsAnalyzing = true)
                val result = backendRepository.analyzeMood(request)

                // 4. Actualizar UI con el resultado del análisis
                if (result != null) {
                    val shaderMood = parseMoodToShader(result.shaderMood)
                    // 🆕 Extraer links como "Artist — Title" para mostrar en UI
                    val links = result.youtubeLinks?.map {
                        "${it.artist} — ${it.songTitle}"
                    } ?: emptyList()
                    Log.d(TAG, "Mood detectado: ${result.mood}, shader=${result.shaderMood}, links=${links.size}")
                    // 🆕 Solo mostrar el 🔮 si el mood es NUEVO (distinto al último visto)
                    val isNewMood = result.mood != lastShownMoodLabel
                    _uiState.value = _uiState.value.copy(
                        moodIsAnalyzing = false,
                        moodLabel = result.mood,
                        moodDescription = result.description,
                        moodShaderSuggestion = shaderMood,
                        moodYoutubeLinks = links,
                        moodViewed = !isNewMood  // false si es nuevo → 🔮 visible
                    )
                    if (isNewMood) {
                        Log.d(TAG, "🔮 Nuevo mood disponible: ${result.mood}")
                    } else {
                        Log.d(TAG, "🔮 Mismo mood que antes, no se muestra")
                    }
                    // 🆕 Limpiar historial tras cada análisis exitoso.
                    // Así el próximo solo envía canciones nuevas, no acumula repetidas.
                    // También reseteamos el tracker local para que la canción actual
                    // se vuelva a registrar en el historial limpio.
                    historyRepository.clearHistory()
                    lastTrackedTrackId = -1
                } else {
                    _uiState.value = _uiState.value.copy(moodIsAnalyzing = false)
                    Log.w(TAG, "Mood: análisis falló o timeout")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en análisis de mood: ${e.message}", e)
                _uiState.value = _uiState.value.copy(moodIsAnalyzing = false)
            }
        }
    }

    /**
     * Convierte el string de shader del backend al enum de ShaderRenderer.
     * Si el backend sugiere "MOIRE_FLOW", el shader cambia automáticamente.
     */
    private fun parseMoodToShader(shaderMood: String?): com.sonus.player.ui.visualizer.ShaderRenderer.Mood? {
        return when (shaderMood?.uppercase()) {
            "MOIRE_FLOW" -> com.sonus.player.ui.visualizer.ShaderRenderer.Mood.MOIRE_FLOW
            "RADIAL_WAVE" -> com.sonus.player.ui.visualizer.ShaderRenderer.Mood.RADIAL_WAVE
            "DIAMOND_GRID" -> com.sonus.player.ui.visualizer.ShaderRenderer.Mood.DIAMOND_GRID
            "INTERFERENCE" -> com.sonus.player.ui.visualizer.ShaderRenderer.Mood.INTERFERENCE
            "XEROGRAPHIC" -> com.sonus.player.ui.visualizer.ShaderRenderer.Mood.XEROGRAPHIC
            else -> null
        }
    }

    /**
     * 🆕 Marca el mood actual como visto. El 🔮 desaparece hasta que
     * llegue un nuevo mood distinto al actual.
     */
    fun markMoodViewed() {
        lastShownMoodLabel = _uiState.value.moodLabel
        _uiState.value = _uiState.value.copy(moodViewed = true)
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
