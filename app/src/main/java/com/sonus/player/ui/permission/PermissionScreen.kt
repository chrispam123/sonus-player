package com.sonus.player.ui.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val audioOnlyPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val fullPermissions = audioOnlyPermissions + Manifest.permission.RECORD_AUDIO

    // 🆕 Controla el diálogo previo que explica por qué pedimos RECORD_AUDIO.
    // El usuario decide si quiere efectos visuales o solo reproducción básica.
    var showPreDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results.entries.any {
            (it.key == Manifest.permission.READ_MEDIA_AUDIO || it.key == Manifest.permission.READ_EXTERNAL_STORAGE) && it.value
        }
        if (audioGranted) {
            onPermissionGranted()
        }
    }

    // 🆕 Diálogo previo: explica RECORD_AUDIO antes de pedirlo al sistema
    if (showPreDialog) {
        AlertDialog(
            onDismissRequest = {
                // Si cierra el diálogo sin elegir, pedimos solo audio
                showPreDialog = false
                launcher.launch(audioOnlyPermissions)
            },
            icon = {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("🎨 Efectos visuales") },
            text = {
                Text(
                    "NO se graba audio del micrófono ni sale nada de tu dispositivo. " +
                    "La música que suena se usa para crear los efectos visuales."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPreDialog = false
                    launcher.launch(fullPermissions)  // Pide audio + RECORD_AUDIO
                }) {
                    Text("Activar efectos")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPreDialog = false
                    launcher.launch(audioOnlyPermissions)  // Solo audio, sin shader
                }) {
                    Text("No, gracias")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sonus",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Para reproducir tu música, Sonus necesita acceso a tus archivos de audio.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { showPreDialog = true }) {
            Text("Permitir acceso")
        }
    }
}
