package com.sonus.player.ui.visualizer

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log
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

private const val TAG = "ShaderCanvas"

@Composable
fun ShaderCanvas(
    fftData: FloatArray,
    amplitude: Float,
    mood: ShaderRenderer.Mood = ShaderRenderer.Mood.XEROGRAPHIC,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember {
        Log.d(TAG, "Creando ShaderRenderer")
        ShaderRenderer()
    }

    renderer.isExiting = false
    renderer.fftData = fftData
    renderer.amplitude = amplitude
    renderer.currentMood = mood

    val renderScope = remember {
        Log.d(TAG, "Creando CoroutineScope")
        CoroutineScope(Dispatchers.Default + Job())
    }

    val listener = remember {
        Log.d(TAG, "Creando SurfaceTextureListener")
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "onSurfaceTextureAvailable: ${width}x${height}")

                try {
                    val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                    Log.d(TAG, "eglGetDisplay: $eglDisplay")
                    if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                        Log.e(TAG, "EGL_NO_DISPLAY")
                        return
                    }

                    val version = IntArray(2)
                    val initOk = EGL14.eglInitialize(eglDisplay, version, 0, version, 0)
                    Log.d(TAG, "eglInitialize: $initOk, version=${version[0]}.${version[1]}")

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
                    Log.d(TAG, "eglChooseConfig: numConfigs=${numConfigs[0]}")

                    val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                    val eglContext = EGL14.eglCreateContext(
                        eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0
                    )
                    Log.d(TAG, "eglCreateContext: $eglContext")

                    val eglSurface = EGL14.eglCreateWindowSurface(
                        eglDisplay, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0
                    )
                    Log.d(TAG, "eglCreateWindowSurface: $eglSurface")

                    val makeCurrentOk = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                    Log.d(TAG, "eglMakeCurrent: $makeCurrentOk")

                    GLES20.glClearColor(0.02f, 0.02f, 0.02f, 1f)
                    renderer.onSurfaceCreated(null, null)
                    renderer.onSurfaceChanged(null, width, height)
                    Log.d(TAG, "Renderer inicializado OK")

                    renderScope.launch {
                        var frameCount = 0
                        Log.d(TAG, "Loop de render iniciado")
                        while (isActive) {
                            renderer.onDrawFrame(null)
                            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                            frameCount++
                            if (frameCount % 60 == 0) {
                                Log.d(TAG, "Render loop: $frameCount frames, amp=$amplitude")
                            }
                            delay(16)
                        }
                        Log.d(TAG, "Loop de render terminado tras $frameCount frames")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en onSurfaceTextureAvailable", e)
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "onSurfaceTextureSizeChanged: ${width}x${height}")
                renderer.onSurfaceChanged(null, width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "onSurfaceTextureDestroyed")
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // no-op
            }
        }
    }

    val textureView = remember {
        Log.d(TAG, "Creando TextureView + asignando listener")
        TextureView(context).apply {
            isOpaque = false
            surfaceTextureListener = listener
        }
    }

    AndroidView(
        factory = {
            Log.d(TAG, "AndroidView factory llamado")
            textureView
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        Log.d(TAG, "DisposableEffect ENTER")
        onDispose {
            Log.d(TAG, "DisposableEffect onDispose")
            renderer.isExiting = true
            renderScope.cancel()
            textureView.surfaceTextureListener = null
        }
    }
}
