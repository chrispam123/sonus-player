package com.sonus.receptor

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.sonus.receptor.service.CacheService
import com.sonus.receptor.service.QueueService
import com.sonus.shared.models.LyricsRequest
import com.sonus.shared.models.LyricsResponse
import com.sonus.shared.models.MoodRequest
import com.sonus.shared.models.MoodResponse
import com.sonus.shared.models.RequestResult
import com.sonus.shared.models.RequestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// ============================================================
// LAMBDA RECEPTOR — Punto de entrada HTTP
// ============================================================
// Esta Lambda es invocada por API Gateway cuando la app Android
// hace requests HTTP. Es la "puerta de entrada" del backend.
//
// Maneja 3 rutas:
//   POST /lyrics  → Solicitar letra de una canción
//   POST /mood    → Solicitar análisis de mood (historial horario)
//   GET  /result  → Consultar estado de una solicitud pendiente
//
// IMPORTANTE: Esta Lambda NO llama a DeepSeek directamente.
// Su trabajo es:
//   1. Validar el request
//   2. Verificar si el resultado ya está en caché (DynamoDB)
//   3. Si no → encolar en SQS para procesamiento asíncrono
//   4. Devolver respuesta inmediata (< 200ms)
// ============================================================

class Handler : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    // --------------------------------------------------------
    // Servicios inyectados
    // --------------------------------------------------------
    // CacheService: lee/escribe en DynamoDB
    // QueueService: envía mensajes a SQS
    // Se instancian una sola vez cuando Lambda está "caliente"
    // (AWS reutiliza la instancia para múltiples invocaciones)
    // --------------------------------------------------------
    private val cacheService = CacheService()
    private val queueService = QueueService()

    // Configuración del serializer JSON
    // ignoreUnknownKeys = true: si el cliente envía campos extra, los ignora
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Punto de entrada principal de la Lambda.
     * API Gateway invoca este método para cada request HTTP.
     *
     * @param input El request HTTP parseado por AWS (path, method, body, headers)
     * @param context Información de ejecución de Lambda (requestId, logs, etc.)
     * @return Respuesta HTTP con statusCode y body JSON
     */
    override fun handleRequest(
        input: APIGatewayV2HTTPEvent,
        context: Context
    ): APIGatewayV2HTTPResponse {

        // HTTP API v2: method y path están en requestContext.http
        // El path incluye el stage (/develop/lyrics). Lo limpiamos.
        val method = input.requestContext.http.method
        val rawPath = input.requestContext.http.path
        val stage = input.requestContext.stage
        // Quitar el prefijo del stage: /develop/lyrics → /lyrics
        val path = if (stage != null) rawPath.removePrefix("/$stage") else rawPath
        context.logger.log("Receptor: $method $path")

        // 🆕 Validar API Key: solo requests con la key correcta pasan.
        // Frena ataques de denegación de wallet (DeepSeek cuesta por token).
        val expectedKey = System.getenv("SONUS_API_KEY")
        if (!expectedKey.isNullOrBlank()) {
            val providedKey = input.headers?.get("x-api-key") ?: ""
            if (providedKey != expectedKey) {
                context.logger.log("Receptor: API Key inválida o ausente")
                return errorResponse(403, "Forbidden: invalid API key")
            }
        }

        return try {
            when {
                method == "POST" && path == "/lyrics" ->
                    handleLyricsRequest(input, context)

                method == "POST" && path == "/mood" ->
                    handleMoodRequest(input, context)

                method == "GET" && path.startsWith("/result/") ->
                    handleResultQuery(input, context)

                else ->
                    errorResponse(404, "Route not found: $method $path")
            }
        } catch (e: Exception) {
            context.logger.log("Receptor ERROR: ${e.message}")
            errorResponse(500, "Internal server error: ${e.message}")
        }
    }

    // ============================================================
    // HANDLER: POST /lyrics
    // ============================================================
    // Flujo:
    //   1. Parsear body JSON a LyricsRequest
    //   2. Verificar caché en DynamoDB (¿ya tenemos esta letra?)
    //   3a. Si SÍ → responder inmediatamente con la letra cacheada
    //   3b. Si NO → generar requestId → encolar en SQS → responder 202
    // ============================================================
    private fun handleLyricsRequest(
        input: APIGatewayV2HTTPEvent,
        context: Context
    ): APIGatewayV2HTTPResponse {

        // Parsear el body JSON
        val request = parseBody<LyricsRequest>(input.body)
            ?: return errorResponse(400, "Invalid request body. Expected: { artist, title }")

        context.logger.log("Lyrics request: ${request.artist} - ${request.title}")

        // Clave de caché: "artista#titulo" (normalizado a lowercase, sin espacios)
        val cacheKey = buildCacheKey(request.artist, request.title)

        // Verificar si ya tenemos la letra en DynamoDB
        val cachedLyrics = cacheService.getLyricsFromCache(cacheKey)

        return if (cachedLyrics != null) {
            // ✅ Cache HIT — responder inmediatamente con la letra guardada
            context.logger.log("Cache HIT for: $cacheKey")
            val response = LyricsResponse(
                requestId = UUID.randomUUID().toString(),
                status = RequestStatus.COMPLETED,
                lyrics = cachedLyrics,
                confidence = "high",
                source = "cache"
            )
            successResponse(200, json.encodeToString(response))

        } else {
            // ❌ Cache MISS — encolar para procesamiento asíncrono
            context.logger.log("Cache MISS for: $cacheKey — enqueueing")

            val requestId = UUID.randomUUID().toString()

            // Guardar estado "pending" en DynamoDB para que el cliente pueda hacer polling
            cacheService.saveRequestStatus(requestId, RequestStatus.PENDING)

            // Encolar mensaje en SQS lyrics_queue
            // La Lambda de Lyrics procesará este mensaje y llamará a DeepSeek
            val message = buildLyricsQueueMessage(requestId, request, cacheKey)
            queueService.sendToLyricsQueue(message)

            // Responder 202 (Accepted) inmediatamente — no esperamos a DeepSeek
            val response = LyricsResponse(
                requestId = requestId,
                status = RequestStatus.PENDING
            )
            successResponse(202, json.encodeToString(response))
        }
    }

    // ============================================================
    // HANDLER: POST /mood
    // ============================================================
    // Flujo similar a /lyrics pero para análisis de mood.
    // La app Android llama esto cada hora con el historial.
    // ============================================================
    private fun handleMoodRequest(
        input: APIGatewayV2HTTPEvent,
        context: Context
    ): APIGatewayV2HTTPResponse {

        val request = parseBody<MoodRequest>(input.body)
            ?: return errorResponse(400, "Invalid request body. Expected: { userId, tracks, periodStart, periodEnd }")

        context.logger.log("Mood request: userId=${request.userId}, tracks=${request.tracks.size}")

        val requestId = UUID.randomUUID().toString()

        // Guardar estado inicial en DynamoDB
        cacheService.saveRequestStatus(requestId, RequestStatus.PENDING)

        // Encolar en SQS mood_queue para procesamiento asíncrono
        val message = buildMoodQueueMessage(requestId, request)
        queueService.sendToMoodQueue(message)

        val response = MoodResponse(
            requestId = requestId,
            status = RequestStatus.PENDING
        )
        return successResponse(202, json.encodeToString(response))
    }

    // ============================================================
    // HANDLER: GET /result/{requestId}
    // ============================================================
    // El cliente hace polling aquí para ver si su solicitud terminó.
    // Devuelve el resultado cuando status == COMPLETED.
    // ============================================================
    private fun handleResultQuery(
        input: APIGatewayV2HTTPEvent,
        context: Context
    ): APIGatewayV2HTTPResponse {

        // HTTP API v2: el path incluye el stage (/develop/result/xyz).
        // Quitamos el stage primero, luego extraemos el requestId.
        val stage = input.requestContext.stage
        var rawPath = input.requestContext.http.path
        if (stage != null) rawPath = rawPath.removePrefix("/$stage")
        val requestId = rawPath.removePrefix("/result/")

        if (requestId.isBlank()) {
            return errorResponse(400, "Missing requestId in path")
        }

        context.logger.log("Polling result for requestId: $requestId")

        // Leer el estado actual de DynamoDB
        val result = cacheService.getRequestResult(requestId)
            ?: return errorResponse(404, "Request not found: $requestId")

        return successResponse(200, json.encodeToString(result))
    }

    // ============================================================
    // HELPERS — Funciones auxiliares
    // ============================================================

    /** Parsea el body JSON del request a una data class */
    private inline fun <reified T> parseBody(body: String?): T? {
        return try {
            if (body.isNullOrBlank()) null
            else json.decodeFromString<T>(body)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Construye una clave de caché normalizada.
     * "Bad Bunny" + "Titi Me Preguntó" → "bad_bunny#titi_me_pregunto"
     * La normalización evita duplicados por mayúsculas/minúsculas.
     */
    private fun buildCacheKey(artist: String, title: String): String {
        val normalizedArtist = artist.lowercase().replace(" ", "_")
        val normalizedTitle = title.lowercase().replace(" ", "_")
        return "$normalizedArtist#$normalizedTitle"
    }

    /**
     * Construye el mensaje JSON que se envía a la cola SQS de letras.
     * Este mensaje es lo que la Lambda de Lyrics recibe para procesar.
     */
    private fun buildLyricsQueueMessage(
        requestId: String,
        request: LyricsRequest,
        cacheKey: String
    ): String = """
        {
            "requestId": "$requestId",
            "artist": "${request.artist}",
            "title": "${request.title}",
            "durationMs": ${request.durationMs},
            "cacheKey": "$cacheKey"
        }
    """.trimIndent()

    /**
     * Construye el mensaje JSON para la cola SQS de mood.
     */
    private fun buildMoodQueueMessage(requestId: String, request: MoodRequest): String =
        """{"requestId": "$requestId", "payload": ${json.encodeToString(request)}}"""

    /** Construye una respuesta HTTP exitosa (v2) */
    private fun successResponse(statusCode: Int, body: String): APIGatewayV2HTTPResponse {
        val resp = APIGatewayV2HTTPResponse()
        resp.statusCode = statusCode
        resp.body = body
        resp.headers = mapOf("Content-Type" to "application/json")
        return resp
    }

    /** Construye una respuesta HTTP de error (v2) */
    private fun errorResponse(statusCode: Int, message: String): APIGatewayV2HTTPResponse {
        val resp = APIGatewayV2HTTPResponse()
        resp.statusCode = statusCode
        resp.body = """{"error": "$message"}"""
        resp.headers = mapOf("Content-Type" to "application/json")
        return resp
    }
}
