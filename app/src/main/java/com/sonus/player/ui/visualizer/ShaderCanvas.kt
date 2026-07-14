package com.sonus.player.ui.visualizer

import android.opengl.GLES20
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
 * the GL rendering lifecycle.
 *
 * FIX #5b (patched): en vez de confiar en un queueEvent{glClear} suelto
 * (que puede ejecutarse sin swapBuffers y no llegar nunca a pantalla),
 * usamos un flag `isExiting` que el propio Renderer.onDrawFrame() respeta.
 * Al salir:
 *   1. Ponemos renderer.isExiting = true
 *   2. Forzamos UN requestRender() final → ese render SÍ hace swapBuffers,
 *      pero dibuja negro en vez del shader (porque onDrawFrame lo consulta)
 *   3. onPause() detiene el hilo GL después de ese frame
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
    // Al salir del player, este LaunchedEffect se cancela automáticamente.
    LaunchedEffect(glSurfaceView) {
        while (true) {
            glSurfaceView.requestRender()
            delay(16) // ~60fps
        }
    }

    // Al salir: marcamos isExiting y forzamos UN último render+swap
    // que dibuja negro en vez del shader.
    DisposableEffect(glSurfaceView) {
        onDispose {
            renderer.isExiting = true
            glSurfaceView.requestRender() // fuerza el frame final (con swap real)
            glSurfaceView.onPause()
        }
    }
}
