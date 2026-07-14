package com.sonus.player.ui.visualizer

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Compose wrapper for GLSurfaceView that renders the Living Canvas shader.
 * Uses RENDERMODE_WHEN_DIRTY + LaunchedEffect timer for full control over
 * the GL rendering lifecycle. When the composable leaves composition:
 *   1. LaunchedEffect cancels → no more requestRender() calls
 *   2. queueEvent clears buffer to black
 *   3. onPause() stops the GL thread
 *   4. SurfaceFlinger receives the cleared frame → shader disappears instantly
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

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            // WHEN_DIRTY: solo renderiza cuando pedimos — no en loop automático.
            // Esto nos da control total sobre cuándo se generan frames GL.
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
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

    // Timer que pide frames GL periódicamente (~60fps).
    // Al salir del player, este LaunchedEffect se cancela automáticamente
    // → no más requestRender() → el hilo GL deja de producir frames.
    LaunchedEffect(glSurfaceView) {
        while (true) {
            glSurfaceView.requestRender()
            delay(16) // ~60fps
        }
    }

    // Al salir: sin requestRender activo, limpiamos y pausamos.
    // El hilo GL recibe glClear, lo ejecuta, y como no hay requestRender
    // pendiente, se pausa sin volver a dibujar el shader encima.
    DisposableEffect(glSurfaceView) {
        onDispose {
            glSurfaceView.queueEvent {
                android.opengl.GLES20.glClearColor(0f, 0f, 0f, 1f)
                android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
            }
            glSurfaceView.onPause()
        }
    }
}
