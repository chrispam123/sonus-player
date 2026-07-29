package com.sonus.player.data.remote.sonus

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// ============================================================
// INTERFACE RETROFIT — Endpoints del backend Sonus
// ============================================================
// Retrofit genera automáticamente la implementación HTTP.
// Cada función mapea 1:1 con los endpoints de API Gateway.
// ============================================================

interface SonusApiService {

    // ── POST /lyrics — Solicitar letra de una canción ──────
    // Responde inmediatamente. Si la letra ya existe en caché
    // DynamoDB, devuelve status=COMPLETED con lyrics.
    // Si no, devuelve status=PENDING y la app hace polling.
    @POST("lyrics")
    suspend fun requestLyrics(
        @Body request: LyricsRequestDto
    ): LyricsResponseDto

    // ── POST /mood — Solicitar análisis de estado de ánimo ──
    // Envía el historial de la última hora.
    // Responde con PENDING, la app hace polling hasta COMPLETED.
    @POST("mood")
    suspend fun requestMood(
        @Body request: MoodRequestDto
    ): MoodResponseDto

    // ── GET /result/{requestId} — Polling de resultados ────
    // La app llama a este endpoint cada ~2 segundos
    // hasta que status cambia de PENDING a COMPLETED/FAILED.
    @GET("result/{requestId}")
    suspend fun getResult(
        @Path("requestId") requestId: String
    ): RequestResultDto
}
