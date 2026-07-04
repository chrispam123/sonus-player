package com.sonus.player.ui.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.domain.controller.PlayerController
import com.sonus.player.domain.model.SyncedLine
import com.sonus.player.domain.model.SyncedLyricsResult
import com.sonus.player.domain.model.LyricsResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val lines: List<SyncedLine> = emptyList(),
    val plainText: String? = null,
    val currentLineIndex: Int = -1,
    val isLoading: Boolean = false,
    val isSynced: Boolean = false,
    val source: String = "",
    val notAvailable: Boolean = false
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private var currentTrackId: Long = -1

    init {
        observeCurrentTrack()
        observeProgress()
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            playerController.currentTrack.collect { track ->
                if (track != null && track.id != currentTrackId) {
                    currentTrackId = track.id
                    fetchLyrics(track)
                }
            }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            playerController.progress.collect { progress ->
                if (_uiState.value.isSynced && _uiState.value.lines.isNotEmpty()) {
                    val currentIndex = findCurrentLine(progress.positionMs)
                    if (currentIndex != _uiState.value.currentLineIndex) {
                        _uiState.value = _uiState.value.copy(currentLineIndex = currentIndex)
                    }
                }
            }
        }
    }

    private fun fetchLyrics(track: Track) {
        viewModelScope.launch {
            _uiState.value = LyricsUiState(isLoading = true)

            // Try synced first
            val syncedResult = lyricsRepository.getSyncedLyrics(track)
            if (syncedResult is SyncedLyricsResult.Found) {
                _uiState.value = LyricsUiState(
                    lines = syncedResult.lines,
                    isSynced = true,
                    source = "LRCLIB"
                )
                return@launch
            }

            // Fallback to plain text
            val plainResult = lyricsRepository.getLyrics(track)
            if (plainResult is LyricsResult.Found) {
                _uiState.value = LyricsUiState(
                    plainText = plainResult.text,
                    isSynced = false,
                    source = "Genius"
                )
                return@launch
            }

            _uiState.value = LyricsUiState(notAvailable = true)
        }
    }

    private fun findCurrentLine(positionMs: Long): Int {
        val lines = _uiState.value.lines
        for (i in lines.indices.reversed()) {
            if (positionMs >= lines[i].startTimeMs) {
                return i
            }
        }
        return -1
    }
}
