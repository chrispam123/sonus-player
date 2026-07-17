package com.sonus.player.data.repository

import android.util.Log
import com.google.gson.Gson
import com.sonus.player.data.remote.sonus.LyricsRequestDto
import com.sonus.player.data.remote.sonus.LyricsResponseDto
import com.sonus.player.data.remote.sonus.MoodRequestDto
import com.sonus.player.data.remote.sonus.MoodResponseDto
import com.sonus.player.data.remote.sonus.SonusApiService
import kotlinx.coroutines.delay
import javax.inject.Inject

// ============================================================
// BACKEND REPOSITORY — Capa de abstracción sobre Sonus API
// ============================================================
// Encapsula el flujo asíncrono del backend (request → polling).
// Los ViewModels no necesitan saber que el backend usa SQS y
// procesamiento diferido. Solo llaman a fetchLyrics() y reciben
// la letra cuando esté lista.
// ============================================================

class BackendRepositoryImpl @Inject constructor(
    private val sonusApi: SonusApiService
) {
    companion object {
        private const val TAG = "BackendRepo"
        private const val MAX_POLLS = 30
        private const val POLL_DELAY_MS = 2000L
    }

    // Gson para parsear el JSON del resultado del polling
    private val gson = Gson()

    // ============================================================
    // FETCH LYRICS — Solicita letra y espera resultado con polling
    // ============================================================
    // Flujo:
    //   1. POST /lyrics → respuesta inmediata (PENDING o COMPLETED)
    //   2. Si COMPLETED → devuelve la letra directamente (cache hit)
    //   3. Si PENDING → polling cada 2s a GET /result/{id}
    //   4. Si FAILED o timeout → devuelve null
    // ============================================================

    suspend fun fetchLyrics(artist: String, title: String): String? {
        return try {
            Log.d(TAG, "Solicitando letra: $artist - $title")

            // 1. Enviar solicitud al backend
            val request = LyricsRequestDto(
                artist = artist,
                title = title
            )
            val response = sonusApi.requestLyrics(request)
            Log.d(TAG, "Respuesta inicial: status=${response.status}, requestId=${response.requestId}")

            // 2. Si ya está en caché DynamoDB → respuesta inmediata
            if (response.status == "COMPLETED") {
                Log.d(TAG, "Cache HIT — letra obtenida de DynamoDB")
                return response.lyrics
            }

            // 3. Polling: esperar a que Lambda Lyrics termine
            Log.d(TAG, "Iniciando polling para requestId=${response.requestId}")
            return pollForResult(response.requestId) { resultData ->
                // El resultado viene como JSON con la letra dentro del campo "data"
                // Por ahora devolvemos el raw — en bloque 4 lo parsearemos al modelo de dominio
                resultData
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo letra: ${e.message}", e)
            null
        }
    }

    // ============================================================
    // ANALYZE MOOD — Envía historial y espera análisis con polling
    // ============================================================
    // Mismo patrón que fetchLyrics pero para análisis de mood.
    // El backend usa DeepSeek para analizar el estado de ánimo
    // basado en el historial de canciones de la última hora.
    // ============================================================

    suspend fun analyzeMood(request: MoodRequestDto): MoodResponseDto? {
        return try {
            Log.d(TAG, "Solicitando análisis de mood: userId=${request.userId}, tracks=${request.tracks.size}")

            // 1. Enviar solicitud al backend
            val response = sonusApi.requestMood(request)
            Log.d(TAG, "Respuesta mood: status=${response.status}")

            // 2. Si ya está listo → respuesta inmediata
            if (response.status == "COMPLETED") return response

            // 3. Polling hasta que DeepSeek termine el análisis.
            // El resultado en DynamoDB es un JSON con los campos:
            // mood, shaderMood, description, youtubeLinks
            val resultJson = pollForResult(response.requestId) { it }
            if (resultJson != null) {
                // Parsear el JSON del resultado de DynamoDB a MoodResponseDto
                gson.fromJson(resultJson, MoodResponseDto::class.java)
            } else null

        } catch (e: Exception) {
            Log.e(TAG, "Error analizando mood: ${e.message}", e)
            null
        }
    }

    // ============================================================
    // POLLING — Espera asíncrona hasta que el backend termine
    // ============================================================
    // Llama a GET /result/{requestId} cada 2 segundos.
    // Si tras MAX_POLLS intentos sigue PENDING → timeout → null.
    // ============================================================

    private suspend fun pollForResult(
        requestId: String,
        extractData: (String) -> String?
    ): String? {
        repeat(MAX_POLLS) { attempt ->
            delay(POLL_DELAY_MS)

            try {
                val result = sonusApi.getResult(requestId)
                Log.d(TAG, "Poll $attempt: status=${result.status}")

                when (result.status) {
                    "COMPLETED" -> {
                        Log.d(TAG, "Resultado COMPLETED para $requestId")
                        return result.data?.let { extractData(it) }
                    }

                    "FAILED" -> {
                        Log.w(TAG, "Resultado FAILED para $requestId: ${result.error}")
                        return null
                    }
                    // "PENDING" → continuar polling
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error en poll $attempt: ${e.message}")
                // Si falla un poll, seguimos intentando (puede ser error de red temporal)
            }
        }

        Log.w(TAG, "Timeout: $MAX_POLLS intentos sin respuesta para $requestId")
        return null
    }
}
