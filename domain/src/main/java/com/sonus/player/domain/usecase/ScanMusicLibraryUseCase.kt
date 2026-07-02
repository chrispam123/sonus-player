package com.sonus.player.domain.usecase

import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.MusicScannerRepository

class ScanMusicLibraryUseCase(private val scanner: MusicScannerRepository) {
    suspend operator fun invoke(): List<Track> = scanner.scanLibrary()
}
