package com.sonus.player.ui.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captures FFT data from the audio output in real-time.
 * Requires RECORD_AUDIO permission and a valid audio session ID.
 */
class AudioVisualizer {

    companion object {
        private const val TAG = "AudioVisualizer"
        private const val CAPTURE_SIZE = 128 // Number of FFT bins
    }

    private var visualizer: Visualizer? = null

    // Normalized frequency magnitudes (0.0 to 1.0) for each band
    private val _fftData = MutableStateFlow(FloatArray(CAPTURE_SIZE / 2) { 0f })
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    // Overall amplitude (0.0 to 1.0)
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    fun start(audioSessionId: Int) {
        try {
            release()
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = CAPTURE_SIZE
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Not used — we use FFT
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft ?: return
                            processFft(fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // ~10-20fps
                    false, // waveform
                    true   // fft
                )
                enabled = true
            }
            Log.d(TAG, "Visualizer started for session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer: ${e.message}")
        }
    }

    private fun processFft(fft: ByteArray) {
        val magnitudes = FloatArray(fft.size / 2)
        var totalAmplitude = 0f

        for (i in magnitudes.indices) {
            val real = fft[i * 2].toFloat()
            val imaginary = fft[i * 2 + 1].toFloat()
            val magnitude = kotlin.math.sqrt(real * real + imaginary * imaginary)
            // Normalize to 0.0 - 1.0 range
            magnitudes[i] = (magnitude / 128f).coerceIn(0f, 1f)
            totalAmplitude += magnitudes[i]
        }

        _fftData.value = magnitudes
        _amplitude.value = (totalAmplitude / magnitudes.size).coerceIn(0f, 1f)
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing visualizer: ${e.message}")
        }
    }
}
