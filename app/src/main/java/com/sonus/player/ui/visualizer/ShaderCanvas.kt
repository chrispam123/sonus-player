package com.sonus.player.ui.visualizer

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

//ahora si
@Composable
fun ShaderCanvas(
    fftData: FloatArray,
    amplitude: Float,
    mood: ShaderRenderer.Mood = ShaderRenderer.Mood.XEROGRAPHIC,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { ShaderRenderer() }

    renderer.isExiting = false
    renderer.fftData = fftData
    renderer.amplitude = amplitude
    renderer.currentMood = mood

    val renderScope = remember { CoroutineScope(Dispatchers.Default + Job()) }

    val listener = remember {
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                EGL14.eglInitialize(eglDisplay, IntArray(2), 0, IntArray(2), 0)

                val configAttribs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

                val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                val eglContext = EGL14.eglCreateContext(
                    eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0
                )
                val eglSurface = EGL14.eglCreateWindowSurface(
                    eglDisplay, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0
                )
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

                GLES20.glClearColor(0.02f, 0.02f, 0.02f, 1f)
                renderer.onSurfaceCreated(null, null)
                renderer.onSurfaceChanged(null, width, height)

                renderScope.launch {
                    while (isActive) {
                        renderer.onDrawFrame(null)
                        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                        delay(16)
                    }
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                renderer.onSurfaceChanged(null, width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // no-op
            }
        }
    }

    val textureView = remember {
        TextureView(context).apply {
            isOpaque = false
            surfaceTextureListener = listener
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            renderer.isExiting = true
            renderScope.cancel()
            textureView.surfaceTextureListener = null
        }
    }
}
