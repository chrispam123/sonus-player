package com.sonus.player.data.remote.sonus

import com.google.gson.annotations.SerializedName

// ============================================================
// DTOs para la comunicación con el backend Sonus (API Gateway)
// ============================================================
// Estos modelos reflejan exactamente el JSON que el backend
// envía/recibe. Los usa Retrofit + Gson automáticamente.
// ============================================================

// ── POST /lyrics — Request ─────────────────────────────────
// Envía artista y título al backend para generar letras con IA
data class LyricsRequestDto(
    val artist: String,
    val title: String,
    @SerializedName("durationMs")
    val durationMs: Long? = null
)

// ── POST /lyrics — Response ─────────────────────────────────
// El backend responde inmediatamente con status PENDING o COMPLETED
data class LyricsResponseDto(
    val requestId: String,
    val status: String,           // "PENDING" | "COMPLETED" | "FAILED"
    val lyrics: String? = null,   // Solo si status = COMPLETED
    val confidence: String? = null,
    val source: String? = null    // "cache" | "deepseek"
)

// ── POST /mood — Request ────────────────────────────────────
// Envía el historial de canciones de la última hora
data class MoodRequestDto(
    val userId: String,
    val tracks: List<TrackSummaryDto>,
    val periodStart: Long,
    val periodEnd: Long
)

data class TrackSummaryDto(
    val title: String,
    val artist: String,
    val genre: String? = null,
    val durationMs: Long,
    val playedAt: Long
)

// ── POST /mood — Response ───────────────────────────────────
data class MoodResponseDto(
    val requestId: String,
    val status: String,
    val mood: String? = null,           // "melancholy" | "energetic" | ...
    val shaderMood: String? = null,     // "MOIRE_FLOW" | "RADIAL_WAVE" | ...
    val description: String? = null,
    val youtubeLinks: List<YoutubeLinkDto>? = null
)

data class YoutubeLinkDto(
    val artist: String,
    val songTitle: String,
    val url: String
)

// ── GET /result/{requestId} — Response ──────────────────────
// Usado para polling: la app pregunta "¿ya terminó mi request?"
data class RequestResultDto(
    val requestId: String,
    val status: String,
    val data: String? = null,     // JSON del resultado (letra o mood)
    val error: String? = null
)
