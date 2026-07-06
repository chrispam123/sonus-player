package com.sonus.player.ui.visualizer

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose wrapper for GLSurfaceView that renders the Living Canvas shader.
 * Shows xerographic moiré patterns that react to audio in real-time.
 */
@Composable
fun ShaderCanvas(
    fftData: FloatArray,
    amplitude: Float,
    mood: ShaderRenderer.Mood = ShaderRenderer.Mood.XEROGRAPHIC,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { ShaderRenderer() }

    // Update renderer data every recomposition
    renderer.fftData = fftData
    renderer.amplitude = amplitude
    renderer.currentMood = mood

    AndroidView(
        factory = {
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        modifier = modifier,
        update = { glSurfaceView ->
            // Data is updated via renderer reference directly
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // GLSurfaceView cleanup handled by Android lifecycle
        }
    }
}
