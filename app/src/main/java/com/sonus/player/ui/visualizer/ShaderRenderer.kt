package com.sonus.player.ui.visualizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 renderer inspired by Bruno Munari's Xerography (1967).
 *
 * Generates moiré interference patterns, radial waves, and crosshatch grids
 * that react to audio FFT data. Each "mood" represents a different zone
 * of Munari's original xerographic artwork.
 */
class ShaderRenderer : GLSurfaceView.Renderer {

    enum class Mood {
        MOIRE_FLOW,     // Curved parallel lines that undulate (upper zone)
        RADIAL_WAVE,    // Concentric waves expanding from center (middle zone)
        DIAMOND_GRID,   // Crosshatch diamond trama (lower zone)
        INTERFERENCE,   // Two overlapping patterns creating moiré (transition)
        XEROGRAPHIC     // Full Munari — all textures combined (complete piece)
    }

    var currentMood: Mood = Mood.XEROGRAPHIC
    var fftData: FloatArray = FloatArray(64) { 0f }
    var amplitude: Float = 0f
    var time: Float = 0f

    // FIX #5b: flag que fuerza un frame negro final al salir del player.
    // Se consulta al inicio de onDrawFrame, ANTES de dibujar el shader.
    @Volatile
    var isExiting: Boolean = false

    // Textura de la imagen de Munari
    private var munariBitmap: Bitmap? = null
    private var munariTextureId: Int = 0

    private var program: Int = 0
    private var vertexBuffer: FloatBuffer? = null

    private var uTimeLocation: Int = 0
    private var uResolutionLocation: Int = 0
    private var uAmplitudeLocation: Int = 0
    private var uBassLocation: Int = 0
    private var uMidLocation: Int = 0
    private var uHighLocation: Int = 0
    private var uMoodLocation: Int = 0
    private var uMunariTextureLocation: Int = 0

    private var width: Float = 1f
    private var height: Float = 1f

    private val vertices = floatArrayOf(
        -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f
    )

    private val vertexShaderCode = """
        attribute vec2 aPosition;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    // Fragment shader inspired by Munari's Xerography 1967
    private val fragmentShaderCode = """
        precision highp float;

        uniform float uTime;
        uniform vec2 uResolution;
        uniform float uAmplitude;
        uniform float uBass;
        uniform float uMid;
        uniform float uHigh;
        uniform int uMood;

        // Textura — imagen de Munari 1967
        uniform sampler2D uMunariTexture;

        // Munari palette: monochrome (white/cream on black)
        vec3 light = vec3(0.85, 0.83, 0.78);  // Cream/paper tone
        vec3 black = vec3(0.02, 0.02, 0.02);  // Deep black

        // ═══════════════════════════════════════════
        // MUNARI BUILDING BLOCKS
        // ═══════════════════════════════════════════

        // Parallel lines with curvature (xerographic scanner effect)
        float parallelLines(vec2 uv, float freq, float curve, float phase) {
            float y_distort = sin(uv.x * curve + phase) * 0.15;
            return sin((uv.y + y_distort) * freq);
        }

        // Radial waves expanding from a point
        float radialWave(vec2 uv, vec2 center, float freq, float speed) {
            float d = length(uv - center);
            return sin(d * freq - uTime * speed);
        }

        // Diamond/crosshatch grid (two diagonal line sets)
        float diamondGrid(vec2 uv, float freq, float distort) {
            float d1 = sin((uv.x + uv.y) * freq + distort);
            float d2 = sin((uv.x - uv.y) * freq - distort * 0.7);
            return d1 * d2;
        }

        // Smooth noise for organic distortion
        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
        }
        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
                f.y
            );
        }

        // ═══════════════════════════════════════════
        // MOOD: MOIRE FLOW (Munari upper zone)
        // Curved parallel lines that undulate with bass
        // ═══════════════════════════════════════════
        float moodMoireFlow(vec2 uv) {
            float freq = 40.0 + uBass * 20.0;
            float curve = 3.0 + uAmplitude * 4.0;

            // Two sets of lines with slightly different frequencies = moiré
            float lines1 = parallelLines(uv, freq, curve, uTime * 0.5);
            float lines2 = parallelLines(uv, freq * 1.05, curve * 0.9, uTime * 0.3 + 1.0);

            // Interference pattern
            float moire = lines1 * lines2;

            // High contrast (Munari style: binary black/white)
            return step(0.0, moire);
        }

        // ═══════════════════════════════════════════
        // MOOD: RADIAL WAVE (Munari center zone)
        // Concentric pulses expanding with beats
        // ═══════════════════════════════════════════
        float moodRadialWave(vec2 uv) {
            vec2 center = vec2(0.5 + sin(uTime * 0.2) * 0.1, 0.5);
            float freq = 25.0 + uMid * 15.0;

            float wave1 = radialWave(uv, center, freq, 2.0 + uBass * 3.0);
            float wave2 = radialWave(uv, center + vec2(0.1, 0.0), freq * 0.95, 1.5);

            // Moiré from two slightly offset radial patterns
            float pattern = wave1 * wave2;

            // Add organic distortion
            float n = noise(uv * 3.0 + uTime * 0.1);
            pattern += n * 0.3 * uAmplitude;

            return step(0.0, pattern);
        }

        // ═══════════════════════════════════════════
        // MOOD: DIAMOND GRID (Munari lower zone)
        // Crosshatch trama that vibrates with highs
        // ═══════════════════════════════════════════
        float moodDiamondGrid(vec2 uv) {
            float freq = 20.0 + uHigh * 10.0;
            float distort = uTime * 0.5 + uBass * 2.0;

            float grid = diamondGrid(uv, freq, distort);

            // Add wave distortion to break regularity
            float wave = sin(uv.y * 8.0 + uTime * 0.3) * uAmplitude * 0.3;
            grid += wave;

            return step(0.1, grid);
        }

        // ═══════════════════════════════════════════
        // MOOD: INTERFERENCE (Munari transition zones)
        // Two patterns fighting for dominance
        // ═══════════════════════════════════════════
        float moodInterference(vec2 uv) {
            // Pattern A: dense parallel lines
            float linesA = sin(uv.y * 50.0 + sin(uv.x * 5.0 + uTime) * (1.0 + uBass * 3.0));

            // Pattern B: slightly rotated lines
            vec2 rotUv = vec2(
                uv.x * 0.97 + uv.y * 0.26,
                uv.y * 0.97 - uv.x * 0.26
            );
            float linesB = sin(rotUv.y * 48.0 + sin(rotUv.x * 4.0 + uTime * 0.7) * (1.0 + uMid * 2.0));

            // Interference = multiplication
            float moire = linesA * linesB;

            // Organic mask
            float mask = noise(uv * 2.0 + uTime * 0.05);
            moire *= (0.7 + mask * 0.6);

            return step(0.0, moire);
        }

        // ═══════════════════════════════════════════
        // MOOD: XEROGRAPHIC (Full Munari — all combined)
        // Zones blend based on position and audio energy
        // ═══════════════════════════════════════════
        float moodXerographic(vec2 uv) {
            // Zone transitions driven by noise + audio
            float zone = uv.y + noise(uv * 2.0 + uTime * 0.1) * 0.3;
            zone += uAmplitude * 0.2;

            float pattern;
            if (zone > 0.65) {
                // Upper: moire flow
                pattern = moodMoireFlow(uv);
            } else if (zone > 0.35) {
                // Middle: radial wave
                pattern = moodRadialWave(uv);
            } else {
                // Lower: diamond grid
                pattern = moodDiamondGrid(uv);
            }

            // Smooth transitions at boundaries
            float edge1 = smoothstep(0.6, 0.7, zone);
            float edge2 = smoothstep(0.3, 0.4, zone);

            // Add interference at transition zones
            if (zone > 0.55 && zone < 0.75) {
                float interf = moodInterference(uv);
                pattern = mix(pattern, interf, 0.5);
            }
            if (zone > 0.25 && zone < 0.45) {
                float interf = moodInterference(uv);
                pattern = mix(pattern, interf, 0.4);
            }

            return pattern;
        }

        // ═══════════════════════════════════════════
        // MAIN
        // ═══════════════════════════════════════════
        void main() {
            vec2 uv = gl_FragCoord.xy / uResolution.xy;

            float pattern;
            if (uMood == 0) pattern = moodMoireFlow(uv);
            else if (uMood == 1) pattern = moodRadialWave(uv);
            else if (uMood == 2) pattern = moodDiamondGrid(uv);
            else if (uMood == 3) pattern = moodInterference(uv);
            else pattern = moodXerographic(uv);

            // Samplear la imagen de Munari con distorsión por audio
            float distort = uAmplitude * 0.015;
            vec2 texUv = uv + vec2(
                sin(uv.y * 10.0 + uTime * 0.3) * distort,
                cos(uv.x * 8.0 + uTime * 0.2) * distort
            );
            vec4 texel = texture2D(uMunariTexture, texUv);

            // Mezcla: la imagen es la base, el patrón procedural modula encima
            float intensity = 0.35 + uAmplitude * 0.45;
            vec3 baseColor = mix(black, light, texel.r * 0.7 + 0.3);
            vec3 patternOverlay = mix(black, light, pattern) * intensity * 0.25;
            vec3 color = baseColor + patternOverlay;

            gl_FragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.02f, 0.02f, 1f)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer?.position(0)

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        uTimeLocation = GLES20.glGetUniformLocation(program, "uTime")
        uResolutionLocation = GLES20.glGetUniformLocation(program, "uResolution")
        uAmplitudeLocation = GLES20.glGetUniformLocation(program, "uAmplitude")
        uBassLocation = GLES20.glGetUniformLocation(program, "uBass")
        uMidLocation = GLES20.glGetUniformLocation(program, "uMid")
        uHighLocation = GLES20.glGetUniformLocation(program, "uHigh")
        uMoodLocation = GLES20.glGetUniformLocation(program, "uMood")
        uMunariTextureLocation = GLES20.glGetUniformLocation(program, "uMunariTexture")

        // Subir la imagen de Munari a la GPU como textura
        loadTextureToGpu()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        width = w.toFloat()
        height = h.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        // FIX #5b: si estamos saliendo del player, dibujamos negro y
        // cortamos ANTES de tocar el program/uniforms/drawArrays del shader.
        // Este frame sí pasa por swapBuffers (a diferencia del queueEvent
        // suelto del fix #5 original), así que sí llega a pantalla.
        if (isExiting) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        time += 0.016f

        val bass = if (fftData.size > 4) fftData.take(4).average().toFloat() else 0f
        val mid = if (fftData.size > 16) fftData.drop(4).take(12).average().toFloat() else 0f
        val high = if (fftData.size > 32) fftData.drop(16).take(16).average().toFloat() else 0f

        GLES20.glUniform1f(uTimeLocation, time)
        GLES20.glUniform2f(uResolutionLocation, width, height)
        GLES20.glUniform1f(uAmplitudeLocation, amplitude)
        GLES20.glUniform1f(uBassLocation, bass)
        GLES20.glUniform1f(uMidLocation, mid)
        GLES20.glUniform1f(uHighLocation, high)
        GLES20.glUniform1i(uMoodLocation, currentMood.ordinal)

        // Vincular textura de Munari a la unidad 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, munariTextureId)
        GLES20.glUniform1i(uMunariTextureLocation, 0)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    /**
     * Carga la imagen de Munari desde resources. Se llama desde ShaderCanvas
     * (main thread). El bitmap se guarda y se sube a la GPU en onSurfaceCreated
     * (hilo GL).
     */
    fun setTextureBitmap(context: Context, resourceId: Int) {
        if (munariBitmap != null) return  // Ya cargada
        munariBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
    }

    /**
     * Sube el bitmap de Munari a la GPU como textura GL.
     * DEBE llamarse desde el hilo GL (dentro de onSurfaceCreated).
     */
    private fun loadTextureToGpu() {
        val bitmap = munariBitmap ?: return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        munariTextureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, munariTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        munariBitmap?.recycle()
        munariBitmap = null  // Liberar memoria RAM, ya está en GPU
    }
}
