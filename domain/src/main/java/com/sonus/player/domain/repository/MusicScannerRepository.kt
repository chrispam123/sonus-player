package com.sonus.player.domain.repository

import com.sonus.player.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicScannerRepository {
    suspend fun scanLibrary(): List<Track>
    fun getScanProgress(): Flow<ScanProgress>
}

data class ScanProgress(
    val totalFiles: Int,
    val processedFiles: Int,
    val isScanning: Boolean
)
