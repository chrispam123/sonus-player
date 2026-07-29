package com.sonus.player.ui.player

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onAddToPlaylist: (trackId: Long, playlistId: Long) -> Unit = { _, _ -> },
    onMoodClick: () -> Unit = {}  // 🆕 Abre MoodDetailScreen al tocar el círculo
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // Audio visualizer data
    val fftData by viewModel.fftData.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🆕 GlowCircle animado — indica que hay un mood NUEVO disponible.
        // Desaparece al tocar, reaparece solo si el próximo análisis es distinto.
        val moodDesc = uiState.moodDescription
        if (moodDesc != null && moodDesc.isNotBlank() && !uiState.moodViewed) {
            val moodColor = moodCircleColor(uiState.moodLabel)
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val pulse by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse"
            )

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(40.dp, 40.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.markMoodViewed(); onMoodClick() }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 * pulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(moodColor, moodColor.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }

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

        // Living Canvas area with cover art on top (fades after 5 seconds)
        Box(
            modifier = Modifier
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: GLSL Shader Canvas (background, full 280dp)
            // 🆕 El mood viene del análisis de DeepSeek cada 30 min.
            // Si no hay mood aún, usa XEROGRAPHIC por defecto.
            val currentMood = uiState.moodShaderSuggestion
                ?: com.sonus.player.ui.visualizer.ShaderRenderer.Mood.XEROGRAPHIC

            com.sonus.player.ui.visualizer.ShaderCanvas(
                fftData = fftData,
                amplitude = amplitude,
                mood = currentMood,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Cover art — fills entire 280dp, fades after 5s to reveal shader
            val coverAlpha = remember { androidx.compose.animation.core.Animatable(1f) }

            // Reset alpha to 1 when track changes, then fade to 0 after 5s
            LaunchedEffect(track?.id) {
                coverAlpha.snapTo(1f)
                kotlinx.coroutines.delay(5000)
                coverAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 1500,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    )
                )
            }

            if (coverAlpha.value > 0f) {
                Box(modifier = Modifier.alpha(coverAlpha.value)) {
                    com.sonus.player.ui.components.CoverArtImage(
                        track = track,
                        size = 280.dp
                    )
                }
            }
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

// 🎨 Color del círculo glow según el mood detectado
private fun moodCircleColor(moodLabel: String?): Color = when (moodLabel) {
    "calm" -> Color(0xFFF4C2C2)       // Rosa pastel
    "romantic" -> Color(0xFFF4C2C2)
    "nostalgic" -> Color(0xFFF4C2C2)
    "energetic" -> Color(0xFFFFD700)  // Amarillo brillante
    "euphoric" -> Color(0xFFFFD700)
    "melancholy" -> Color(0xFFC4B5FD) // Lavanda
    "sad" -> Color(0xFFC4B5FD)
    "happy" -> Color(0xFFFF6B9D)      // Rosa chicle
    "joyful" -> Color(0xFFFF6B9D)
    "focused" -> Color(0xFF98FB98)    // Verde menta
    "tense" -> Color(0xFF98FB98)
    else -> Color(0xFFD4A0A0)          // Dusty Rose (fallback)
}
