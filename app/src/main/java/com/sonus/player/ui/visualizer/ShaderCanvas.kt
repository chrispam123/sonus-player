package com.sonus.player.ui.visualizer

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "ShaderCanvas"

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
        Log.d(TAG, "CREATE GLSurfaceView")
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            Log.d(TAG, "renderMode=CONTINUOUSLY, renderer=${System.identityHashCode(renderer)}")
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
        Log.d(TAG, "ENTER composicion, glSurfaceView=${System.identityHashCode(glSurfaceView)}")
        onDispose {
            Log.d(TAG, "onDispose -> removiendo del parent y pausando GL...")
            // Remover del AndroidView wrapper inmediatamente.
            // visibility=GONE no basta: el wrapper de Compose retiene el último frame.
            (glSurfaceView.parent as? android.view.ViewGroup)?.removeView(glSurfaceView)
            glSurfaceView.onPause()
            Log.d(TAG, "removeView + onPause() completado")
        }
    }
}
