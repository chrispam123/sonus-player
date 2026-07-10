package com.sonus.lyrics

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.sonus.lyrics.service.DeepSeekService
import com.sonus.lyrics.service.LyricsCacheService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ============================================================
// LAMBDA LYRICS PROCESSOR — Genera letras con DeepSeek
// ============================================================
// Esta Lambda es disparada automáticamente por SQS cuando
// el Receptor encola una solicitud de letra.
//
// Input: SQSEvent — puede contener 1 o más mensajes a la vez
//   AWS puede hacer "batching" y enviar hasta 10 mensajes
//   en una sola invocación para optimizar costos.
//
// Flujo por mensaje:
//   1. Parsear el mensaje JSON de SQS
//   2. Llamar a DeepSeek API con el artista y título
//   3. Guardar la letra en DynamoDB (cache + status)
//   4. Si falla → marcar status como FAILED (el cliente verá el error)
// ============================================================
class Handler : RequestHandler<SQSEvent, Unit> {

    // Servicios que se inicializan una vez por instancia de Lambda
    private val deepSeekService = DeepSeekService()
    private val cacheService = LyricsCacheService()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Punto de entrada — SQS invoca este método con un batch de mensajes.
     *
     * @param input SQSEvent con lista de mensajes de la cola lyrics_queue
     * @param context Información de ejecución (logs, timeout, etc.)
     */
    override fun handleRequest(input: SQSEvent, context: Context) {
        context.logger.log("Lyrics processor: recibidos ${input.records.size} mensajes")

        // Procesar cada mensaje del batch
        // Si uno falla, SQS lo reintentará automáticamente
        input.records.forEach { record ->
            processRecord(record, context)
        }
    }

    /**
     * Procesa un mensaje individual de la cola SQS.
     *
     * @param record Un mensaje SQS individual
     * @param context Contexto de Lambda para logging
     */
    private fun processRecord(record: SQSEvent.SQSMessage, context: Context) {
        val requestId: String
        val artist: String
        val title: String
        val cacheKey: String

        try {
            // Parsear el JSON del mensaje
            // Formato: { "requestId": "...", "artist": "...", "title": "...", "cacheKey": "..." }
            val messageJson = json.parseToJsonElement(record.body).jsonObject
            requestId = messageJson["requestId"]!!.jsonPrimitive.content
            artist = messageJson["artist"]!!.jsonPrimitive.content
            title = messageJson["title"]!!.jsonPrimitive.content
            cacheKey = messageJson["cacheKey"]!!.jsonPrimitive.content

            context.logger.log("Procesando letra: $artist - $title (requestId: $requestId)")
        } catch (e: Exception) {
            // Si el mensaje está malformado, logueamos y descartamos
            // No podemos hacer mucho sin el requestId para actualizar el status
            context.logger.log("ERROR: mensaje SQS malformado: ${record.body}")
            return
        }

        try {
            // Llamar a DeepSeek para generar la letra
            // Esta es la operación más lenta (~3-8 segundos)
            context.logger.log("Llamando a DeepSeek para: $artist - $title")
            val lyrics = deepSeekService.generateLyrics(artist, title)

            // Guardar la letra en DynamoDB
            // 1. En la tabla de caché (para futuras solicitudes del mismo tema)
            // 2. Actualizar el status de la solicitud a COMPLETED
            cacheService.saveLyrics(cacheKey, lyrics)
            cacheService.updateRequestStatus(
                requestId = requestId,
                status = "COMPLETED",
                data = lyrics
            )
            context.logger.log("Letra guardada para: $artist - $title")

        } catch (e: Exception) {
            // Error al generar o guardar la letra
            // Actualizar status a FAILED para que el cliente lo sepa
            context.logger.log("ERROR generando letra para $artist - $title: ${e.message}")
            cacheService.updateRequestStatus(
                requestId = requestId,
                status = "FAILED",
                error = e.message ?: "Unknown error"
            )
            // Re-lanzar la excepción para que SQS reintente el mensaje
            // SQS tiene configurado maxReceiveCount = 3 (ver Terraform)
            throw e
        }
    }
}
