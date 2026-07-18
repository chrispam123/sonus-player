package com.sonus.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// MOOD DETAIL SCREEN — Pantalla completa de análisis de mood
// ============================================================
// Se abre al tocar el círculo glow en NowPlayingScreen.
// Muestra: etiqueta del mood, descripción, links sugeridos.
// El fondo cambia de color según el tipo de música.
// ============================================================

// 🎨 Mapeo de moods a colores de fondo
private fun moodColor(moodLabel: String?): Color = when {
    moodLabel == null -> Color(0xFF2D2D2D)  // Gris oscuro neutro
    moodLabel == "calm" || moodLabel == "romantic" || moodLabel == "nostalgic" ->
        Color(0x33F4C2C2)  // Rosa pastel semitransparente sobre fondo oscuro
    moodLabel == "energetic" || moodLabel == "euphoric" ->
        Color(0x33FFD700)  // Amarillo brillante
    moodLabel == "melancholy" || moodLabel == "sad" ->
        Color(0x33C4B5FD)  // Lavanda
    moodLabel == "happy" || moodLabel == "joyful" ->
        Color(0x33FF6B9D)  // Rosa chicle
    moodLabel == "focused" || moodLabel == "tense" ->
        Color(0x3398FB98)  // Verde menta
    else -> Color(0x33B0B0B0)  // Gris neutro
}

@Composable
fun MoodDetailScreen(
    moodLabel: String?,
    moodDescription: String?,
    moodLinks: List<String>,
    onClose: () -> Unit
) {
    val bgColor = moodColor(moodLabel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Capa decorativa: círculo grande difuminado arriba
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopCenter)
                .offset(x = 0.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(bgColor.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón volver
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 🎭 Mood label — grande, centrado
            val label = moodLabel?.replaceFirstChar { it.uppercase() } ?: "Sin análisis"
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 💭 Descripción
            if (!moodDescription.isNullOrBlank()) {
                Text(
                    text = moodDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 🔗 Links sugeridos (hasta 4)
            if (moodLinks.isNotEmpty()) {
                Text(
                    text = "También te podría gustar",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                moodLinks.take(4).forEach { link ->
                    Text(
                        text = "🔗 $link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
