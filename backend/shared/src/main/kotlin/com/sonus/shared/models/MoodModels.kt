package com.sonus.shared.models

import kotlinx.serialization.Serializable

// ============================================================
// MOOD MODELS — Contrato para el análisis de estado de ánimo
// ============================================================
// El análisis de mood ocurre cada hora.
// La app envía las canciones escuchadas → el backend analiza
// el patrón con DeepSeek → devuelve mood + sugerencias.
// ============================================================

/**
 * Una canción del historial de escucha.
 * La app Android envía esto para el análisis de mood.
 */
@Serializable
data class TrackSummary(
    val title: String,
    val artist: String,
    val genre: String? = null,
    val durationMs: Long,
    val playedAt: Long    // Timestamp epoch cuando fue escuchada
)

/**
 * Lo que la app envía para solicitar un análisis de mood.
 *
 * Se envía automáticamente cada hora con las canciones de esa hora.
 *
 * Ejemplo JSON:
 * {
 *   "userId": "user_1",
 *   "tracks": [
 *     { "title": "Creep", "artist": "Radiohead", "durationMs": 238000, "playedAt": 1720000000 },
 *     { "title": "Summertime Sadness", "artist": "Lana Del Rey", "durationMs": 270000, "playedAt": 1720003600 }
 *   ],
 *   "periodStart": 1720000000,
 *   "periodEnd": 1720003600
 * }
 */
@Serializable
data class MoodRequest(
    val userId: String,            // Por ahora siempre "user_1" (app personal)
    val tracks: List<TrackSummary>,
    val periodStart: Long,         // Inicio del período analizado (epoch ms)
    val periodEnd: Long            // Fin del período analizado (epoch ms)
)

/**
 * Lo que el backend devuelve después de analizar el mood.
 *
 * Ejemplo JSON:
 * {
 *   "requestId": "xyz-456",
 *   "status": "completed",
 *   "mood": "melancholy",
 *   "shaderMood": "MOIRE_FLOW",
 *   "description": "Pareces estar en un momento reflexivo...",
 *   "youtubeLinks": [
 *     { "artist": "The XX", "url": "https://youtube.com/..." },
 *     { "artist": "Daughter", "url": "https://youtube.com/..." }
 *   ]
 * }
 */
@Serializable
data class MoodResponse(
    val requestId: String,
    val status: RequestStatus,
    val mood: String? = null,              // "melancholy" | "energetic" | "calm" | etc.
    val shaderMood: String? = null,        // "MOIRE_FLOW" | "RADIAL_WAVE" | etc.
    val description: String? = null,       // Texto descriptivo para mostrar al usuario
    val youtubeLinks: List<YoutubeLink>? = null
)

/**
 * Un link de YouTube sugerido basado en el análisis de mood.
 */
@Serializable
data class YoutubeLink(
    val artist: String,
    val songTitle: String,
    val url: String
)
