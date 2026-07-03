package com.sonus.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import com.sonus.player.domain.repository.MusicScannerRepository
import com.sonus.player.data.repository.MusicRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicScanner: MusicScannerRepository,
    private val musicRepositoryImpl: MusicRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeTracks()
        scanLibrary() // Auto-scan on first access
    }

    private fun observeTracks() {
        viewModelScope.launch {
            musicRepository.getAllTracks().collect { tracks ->
                _uiState.value = _uiState.value.copy(
                    tracks = tracks,
                    isLoading = false
                )
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                val scannedTracks = musicScanner.scanLibrary()
                musicRepositoryImpl.indexScannedTracks(scannedTracks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }
}
