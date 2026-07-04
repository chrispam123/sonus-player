package com.sonus.player.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cover art
        com.sonus.player.ui.components.CoverArtImage(
            track = track,
            size = 250.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Track info
        Text(
            text = track?.title ?: "Sin reproducción",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = track?.artist ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Seekbar
        val progress = uiState.progress
        val sliderPosition = if (progress.durationMs > 0) {
            progress.positionMs.toFloat() / progress.durationMs.toFloat()
        } else 0f

        Slider(
            value = sliderPosition,
            onValueChange = { fraction ->
                val newPosition = (fraction * progress.durationMs).toLong()
                viewModel.seekTo(newPosition)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(progress.positionMs),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatDuration(progress.durationMs),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previous() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = { viewModel.next() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
