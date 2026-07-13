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
 *
 * IMPORTANTE: GLSurfaceView se crea en remember() y se pausa en onDispose.
 * Esto garantiza que el hilo de render GL se detenga inmediatamente
 * al navegar fuera del player, sin el retraso de ~4s del GC.
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

    // GLSurfaceView en remember: misma instancia durante toda la vida del composable.
    // Al salir de composición, DisposableEffect.onDispose llama a onPause()
    // y detiene el hilo de render inmediatamente.
    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    // Actualizar datos del renderer en cada recomposición
    renderer.fftData = fftData
    renderer.amplitude = amplitude
    renderer.currentMood = mood

    AndroidView(
        factory = { glSurfaceView },
        modifier = modifier
    )

    // Al salir del player: pausar el hilo GL inmediatamente.
    // Sin esto, el hilo sigue corriendo ~4s generando frames invisibles.
    DisposableEffect(glSurfaceView) {
        onDispose {
            glSurfaceView.onPause()
        }
    }
}
