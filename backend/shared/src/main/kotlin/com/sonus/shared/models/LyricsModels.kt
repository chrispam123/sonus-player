package com.sonus.shared.models

import kotlinx.serialization.Serializable

// ============================================================
// LYRICS MODELS — Contrato de comunicación para letras
// ============================================================
// Estos tipos definen exactamente qué envía la app Android
// y qué devuelve el backend.
// Al ser Kotlin en ambos lados, el compilador garantiza
// que si cambias un tipo, tienes que actualizarlo en todos
// los lugares que lo usan.
// ============================================================

/**
 * Lo que la app Android envía al backend para pedir una letra.
 *
 * Ejemplo de JSON enviado:
 * {
 *   "artist": "Bad Bunny",
 *   "title": "Titi Me Preguntó",
 *   "durationMs": 225000
 * }
 */
@Serializable
data class LyricsRequest(
    val artist: String,
    val title: String,
    val durationMs: Long? = null   // Opcional — ayuda a DeepSeek a confirmar la canción
)

/**
 * Lo que el backend devuelve al recibir una solicitud de letra.
 * La respuesta es INMEDIATA (no espera a DeepSeek).
 *
 * Si la letra ya estaba en caché → status = "completed" y se incluye el texto.
 * Si no → status = "pending" y la app debe hacer polling con el requestId.
 *
 * Ejemplo JSON (letra cacheada):
 * { "requestId": "abc-123", "status": "completed", "lyrics": "Ay, vente pa'cá..." }
 *
 * Ejemplo JSON (procesando):
 * { "requestId": "abc-123", "status": "pending" }
 */
@Serializable
data class LyricsResponse(
    val requestId: String,             // UUID para hacer polling
    val status: RequestStatus,
    val lyrics: String? = null,        // Solo presente si status = COMPLETED
    val confidence: String? = null,    // "high" | "medium" | "low"
    val source: String? = null         // "cache" | "deepseek"
)

/**
 * Resultado de hacer polling con GET /result/{requestId}
 */
@Serializable
data class RequestResult(
    val requestId: String,
    val status: RequestStatus,
    val data: String? = null,     // JSON del resultado (letra o mood)
    val error: String? = null     // Mensaje de error si status = FAILED
)

/**
 * Estados posibles de una solicitud asíncrona.
 */
@Serializable
enum class RequestStatus {
    PENDING,    // La Lambda Receptor lo encoló en SQS, esperando procesamiento
    COMPLETED,  // La Lambda Processor terminó y guardó el resultado
    FAILED      // Algo salió mal (DeepSeek no respondió, etc.)
}
