package com.sonus.player.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.Track

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playlists: List<Playlist> = emptyList(),
    onTrackClick: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (trackId: Long, playlistId: Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isScanning -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Escaneando música...")
                }
            }
            uiState.tracks.isEmpty() -> {
                Text(
                    text = "No se encontró música",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.tracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            onClick = { onTrackClick(track, uiState.tracks) },
                            onLongClick = { trackForPlaylist = track }
                        )
                    }
                }
            }
        }
    }

    // Add to playlist dialog
    trackForPlaylist?.let { track ->
        AddToPlaylistDialog(
            trackName = track.title,
            playlists = playlists,
            onDismiss = { trackForPlaylist = null },
            onSelect = { playlistId ->
                onAddToPlaylist(track.id, playlistId)
                trackForPlaylist = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.sonus.player.ui.components.CoverArtImage(
            track = track,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    trackName: String,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("No hay playlists. Crea una primero desde la pestaña Playlists.")
            } else {
                Column {
                    Text("\"$trackName\"", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onSelect(playlist.id) },
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
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
