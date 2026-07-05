package com.sonus.player.ui.playlist

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.sonus.player.domain.model.Playlist
import com.sonus.player.ui.theme.CyberLime
import com.sonus.player.ui.theme.DeepGray
import com.sonus.player.ui.theme.InkBlack
import com.sonus.player.ui.theme.MediumGray
import com.sonus.player.ui.theme.SoftGray

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel,
    onPlaylistClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = CyberLime,
                contentColor = InkBlack,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Crear lista")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "COLECCIONES",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CyberLime,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ARCHIVO PERSONAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.playlists.isEmpty()) {
                Text(
                    text = "No hay listas. Crea una con el botón +",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftGray
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.playlists, key = { it.id }) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            onDelete = { viewModel.deletePlaylist(playlist.id) }
                        )
                    }
                }
            }
        }
    }

    // Create dialog
    if (uiState.showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name -> viewModel.createPlaylist(name) }
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${playlist.trackCount} PISTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Eliminar",
                    tint = SoftGray,
                    modifier = androidx.compose.ui.Modifier.width(20.dp)
                )
            }
        }
        HorizontalDivider(color = MediumGray, thickness = 0.5.dp)
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepGray,
        title = { Text("Nueva lista", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberLime,
                    cursorColor = CyberLime,
                    focusedLabelColor = CyberLime
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("CREAR", color = if (name.isNotBlank()) CyberLime else SoftGray) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = SoftGray) }
        }
    )
}
