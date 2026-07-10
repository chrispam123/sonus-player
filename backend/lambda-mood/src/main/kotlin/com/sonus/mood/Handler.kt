package com.sonus.mood

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.sonus.mood.service.MoodAnalyzerService
import com.sonus.mood.service.MoodStorageService
import com.sonus.shared.models.MoodRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ============================================================
// LAMBDA MOOD ANALYZER — Analiza estado de ánimo con IA
// ============================================================
// Esta Lambda se dispara por SQS mood_queue.
// Recibe el historial de canciones de la última hora y
// usa DeepSeek para determinar el estado de ánimo del usuario.
//
// El resultado incluye:
//   - mood: etiqueta del estado ("melancholy", "energetic", etc.)
//   - shaderMood: qué patrón visual mostrar en el Player
//   - description: texto descriptivo para mostrar en la app
//   - youtubeLinks: sugerencias de artistas similares
//
// Este análisis se guarda en DynamoDB y la app Android
// lo recupera via GET /result/{requestId}
// ============================================================
class Handler : RequestHandler<SQSEvent, Unit> {

    private val analyzerService = MoodAnalyzerService()
    private val storageService = MoodStorageService()
    private val json = Json { ignoreUnknownKeys = true }

    override fun handleRequest(input: SQSEvent, context: Context) {
        context.logger.log("Mood analyzer: ${input.records.size} mensajes")

        input.records.forEach { record ->
            processRecord(record, context)
        }
    }

    private fun processRecord(record: SQSEvent.SQSMessage, context: Context) {
        val requestId: String
        val moodRequest: MoodRequest

        try {
            // Parsear el mensaje: { "requestId": "...", "payload": { MoodRequest } }
            val messageJson = json.parseToJsonElement(record.body).jsonObject
            requestId = messageJson["requestId"]!!.jsonPrimitive.content
            val payloadStr = messageJson["payload"].toString()
            moodRequest = json.decodeFromString(payloadStr)

            context.logger.log("Analizando mood: userId=${moodRequest.userId}, tracks=${moodRequest.tracks.size}")
        } catch (e: Exception) {
            context.logger.log("ERROR: mensaje SQS malformado: ${e.message}")
            return
        }

        try {
            // Llamar a DeepSeek para analizar el mood
            context.logger.log("Llamando a DeepSeek para análisis de mood...")
            val moodResult = analyzerService.analyzeMood(moodRequest)

            // Guardar resultado en DynamoDB
            storageService.saveMoodResult(requestId, moodRequest.userId, moodResult)
            context.logger.log("Mood guardado: ${moodResult.mood} para userId=${moodRequest.userId}")

        } catch (e: Exception) {
            context.logger.log("ERROR analizando mood: ${e.message}")
            storageService.updateRequestFailed(requestId, e.message ?: "Unknown error")
            throw e // Re-lanzar para retry de SQS
        }
    }
}
