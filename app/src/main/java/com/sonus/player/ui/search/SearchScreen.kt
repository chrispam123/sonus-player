package com.sonus.player.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonus.player.domain.model.Track
import com.sonus.player.ui.components.CoverArtImage
import com.sonus.player.ui.theme.CyberLime
import com.sonus.player.ui.theme.SoftGray

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackClick: (Track) -> Unit,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar artista o canción...", color = SoftGray) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SoftGray) },
            trailingIcon = {
                IconButton(onClick = {
                    if (uiState.query.isNotEmpty()) {
                        viewModel.onQueryChanged("")
                    } else {
                        onClose()
                    }
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = SoftGray)
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberLime,
                cursorColor = CyberLime,
                focusedLeadingIconColor = CyberLime
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Source indicator
        Text(
            text = "CONTENIDO LIBRE DE DERECHOS DE AUTOR",
            style = MaterialTheme.typography.labelSmall,
            color = SoftGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isSearching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CyberLime
                    )
                }
                uiState.hasSearched && uiState.results.isEmpty() -> {
                    Text(
                        text = "Sin resultados para \"${uiState.query}\"",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftGray
                    )
                }
                uiState.results.isNotEmpty() -> {
                    LazyColumn {
                        items(uiState.results, key = { it.id }) { track ->
                            SearchResultItem(
                                track = track,
                                onClick = { onTrackClick(track) }
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Descubre música libre.\nEscribe un artista o género.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArtImage(track = track, size = 48.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
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
        // Duration in Cyber Lime
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.labelMedium,
            color = CyberLime
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
