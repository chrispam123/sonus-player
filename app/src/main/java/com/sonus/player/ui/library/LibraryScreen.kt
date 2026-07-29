package com.sonus.player.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonus.player.domain.model.Playlist
import com.sonus.player.domain.model.Track
import com.sonus.player.ui.theme.CyberLime
import com.sonus.player.ui.theme.JetBrainsMono
import com.sonus.player.ui.theme.SoftGray

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playlists: List<Playlist> = emptyList(),
    onTrackClick: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (trackId: Long, playlistId: Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("PISTAS", "ÁLBUMES", "ARTISTAS")

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: MI BIBLIOTECA + count
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "MI BIBLIOTECA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "■ ${uiState.tracks.size} PISTAS EN ARCHIVO",
                style = MaterialTheme.typography.labelMedium,
                color = SoftGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shuffle button
        Button(
            onClick = {
                if (uiState.tracks.isNotEmpty()) {
                    val shuffled = uiState.tracks.shuffled()
                    onTrackClick(shuffled.first(), shuffled)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberLime,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "REPRODUCCIÓN ALEATORIA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: PISTAS | ÁLBUMES | ARTISTAS
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = CyberLime,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberLime
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == index) CyberLime else SoftGray
                        )
                    }
                )
            }
        }

        // Content based on tab
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isScanning -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CyberLime)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Escaneando música...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                uiState.tracks.isEmpty() -> {
                    Text(
                        text = "No se encontró música",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftGray
                    )
                }
                else -> {
                    when (selectedTab) {
                        0 -> TrackList(
                            tracks = uiState.tracks,
                            onTrackClick = onTrackClick,
                            onLongClick = { trackForPlaylist = it }
                        )
                        1 -> AlbumList(tracks = uiState.tracks, onTrackClick = onTrackClick)
                        2 -> ArtistList(tracks = uiState.tracks, onTrackClick = onTrackClick)
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

@Composable
private fun TrackList(
    tracks: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onLongClick: (Track) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tracks, key = { it.id }) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClick(track, tracks) },
                onLongClick = { onLongClick(track) }
            )
        }
    }
}

@Composable
private fun AlbumList(
    tracks: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    val albums = tracks.groupBy { it.album }.entries.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums, key = { it.key }) { (album, albumTracks) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onTrackClick(albumTracks.first(), albumTracks) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.sonus.player.ui.components.CoverArtImage(
                    track = albumTracks.first(),
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${albumTracks.size} PISTAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistList(
    tracks: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    val artists = tracks.groupBy { it.artist }.entries.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.key }) { (artist, artistTracks) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onTrackClick(artistTracks.first(), artistTracks) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${artistTracks.size} PISTAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column {
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
                    text = track.artist.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Duration in Cyber Lime monospace
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.labelMedium,
                color = CyberLime
            )
        }
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
                Text("No hay playlists. Crea una desde la pestaña Listas.")
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
    return "%02d:%02d".format(minutes, seconds)
}
