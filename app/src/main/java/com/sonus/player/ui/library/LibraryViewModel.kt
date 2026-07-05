package com.sonus.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicRepository
import com.sonus.player.domain.repository.MusicScannerRepository
import com.sonus.player.data.repository.MusicRepositoryImpl
import com.sonus.player.data.local.dao.TrackDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicScanner: MusicScannerRepository,
    private val musicRepositoryImpl: MusicRepositoryImpl,
    private val trackDao: TrackDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeTracks()
        scanIfEmpty() // Only scan if database is empty
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

    /**
     * Only scan MediaStore if the database has no tracks.
     * This prevents re-scanning every time the user navigates to Library.
     */
    private fun scanIfEmpty() {
        viewModelScope.launch {
            val count = trackDao.getTrackCount()
            if (count == 0) {
                scanLibrary()
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
