package com.sonus.player.ui.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.sonus.player.ui.theme.CyberLime
import kotlin.math.sin

/**
 * Living Canvas — Xerographic moiré pattern that reacts to audio frequencies.
 *
 * Draws intersecting line patterns that distort based on FFT data.
 * The result is an organic, breathing visual that makes the sound visible.
 */
@Composable
fun LivingCanvas(
    fftData: FloatArray,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    // Continuous animation frame
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "canvas")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 100000,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val lineCount = 40
        val spacing = height / lineCount

        // Base color with alpha modulated by amplitude
        val baseAlpha = 0.15f + (amplitude * 0.4f)
        val accentAlpha = 0.05f + (amplitude * 0.3f)

        // Layer 1: Horizontal lines with wave distortion
        for (i in 0 until lineCount) {
            val y = i * spacing
            val fftIndex = (i * fftData.size / lineCount).coerceIn(0, fftData.size - 1)
            val freq = if (fftData.isNotEmpty()) fftData[fftIndex] else 0f

            // Distortion based on audio frequency
            val distortion = freq * 30f
            val waveOffset = sin((i + time * 2f) * 0.3f).toFloat() * (5f + distortion)

            drawLine(
                color = CyberLime.copy(alpha = baseAlpha * (0.5f + freq)),
                start = Offset(0f, y + waveOffset),
                end = Offset(width, y + waveOffset + distortion),
                strokeWidth = 1f + freq * 2f,
                cap = StrokeCap.Butt
            )
        }

        // Layer 2: Vertical lines (creates moiré intersection)
        val verticalCount = 25
        val vSpacing = width / verticalCount
        for (i in 0 until verticalCount) {
            val x = i * vSpacing
            val fftIndex = (i * fftData.size / verticalCount).coerceIn(0, fftData.size - 1)
            val freq = if (fftData.isNotEmpty()) fftData[fftIndex] else 0f

            val distortion = freq * 20f
            val waveOffset = sin((i + time * 1.5f) * 0.4f).toFloat() * (3f + distortion)

            drawLine(
                color = Color.White.copy(alpha = accentAlpha * (0.3f + freq * 0.7f)),
                start = Offset(x + waveOffset, 0f),
                end = Offset(x + waveOffset + distortion, height),
                strokeWidth = 0.5f + freq * 1.5f,
                cap = StrokeCap.Butt
            )
        }

        // Layer 3: Diagonal accent lines (sparse, appear on strong beats)
        if (amplitude > 0.3f) {
            val diagCount = 8
            for (i in 0 until diagCount) {
                val progress = i.toFloat() / diagCount
                val fftIndex = (i * fftData.size / diagCount).coerceIn(0, fftData.size - 1)
                val freq = if (fftData.isNotEmpty()) fftData[fftIndex] else 0f

                if (freq > 0.4f) {
                    val startX = width * progress
                    val startY = 0f
                    val endX = width * (1f - progress)
                    val endY = height

                    drawLine(
                        color = CyberLime.copy(alpha = freq * 0.3f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1f,
                        cap = StrokeCap.Butt
                    )
                }
            }
        }
    }
}
