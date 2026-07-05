package com.sonus.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.ui.theme.CyberLime
import com.sonus.player.ui.theme.DeepGray
import com.sonus.player.ui.theme.InkBlack
import com.sonus.player.ui.theme.JetBrainsMono
import com.sonus.player.ui.theme.SoftGray

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onLyricsClick: () -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    onAddToPlaylist: (trackId: Long, playlistId: Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action buttons above cover art (between SONUS topbar and cover)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { showPlaylistDialog = true }) {
                Text("+ LISTA", style = MaterialTheme.typography.labelMedium, color = SoftGray)
            }
            TextButton(onClick = onLyricsClick) {
                Text("LETRAS", style = MaterialTheme.typography.labelMedium, color = CyberLime)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cover art in card style
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(DeepGray),
            contentAlignment = Alignment.Center
        ) {
            com.sonus.player.ui.components.CoverArtImage(
                track = track,
                size = 280.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Technical info (monospace) — format, bitrate
        if (track != null) {
            Text(
                text = "${track.format.name} • ${track.sampleRate}HZ",
                style = MaterialTheme.typography.labelSmall,
                color = SoftGray,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track title — bold, centered
        Text(
            text = track?.title ?: "Sin reproducción",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist — uppercase, letterspaced
        Text(
            text = track?.artist?.uppercase() ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = SoftGray,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Seekbar — thin line, Cyber Lime
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
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CyberLime,
                activeTrackColor = CyberLime,
                inactiveTrackColor = SoftGray.copy(alpha = 0.3f)
            )
        )

        // Timestamps — monospace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(progress.positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = SoftGray
            )
            Text(
                text = formatDuration(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = SoftGray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls row: shuffle | prev | PLAY | next | repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.shuffleEnabled) CyberLime else SoftGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(onClick = { viewModel.previous() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play/Pause — Square Cyber Lime button
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(56.dp)
                    .background(CyberLime)
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(32.dp),
                    tint = InkBlack
                )
            }

            // Next
            IconButton(onClick = { viewModel.next() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                Icon(
                    imageVector = when (uiState.repeatMode) {
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (uiState.repeatMode != RepeatMode.OFF) CyberLime else SoftGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Add to playlist dialog
    if (showPlaylistDialog && track != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Agregar a lista") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No hay listas. Crea una desde la pestaña Listas.")
                } else {
                    Column {
                        Text("\"${track.title}\"", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(track.id, playlist.id)
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(playlist.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
