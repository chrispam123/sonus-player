package com.sonus.mood.service

import com.sonus.shared.models.MoodResponse
import com.sonus.shared.models.RequestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

// ============================================================
// MOOD STORAGE SERVICE — Persistencia del análisis de mood
// ============================================================
// Guarda los resultados del análisis en dos tablas DynamoDB:
//
//   1. sonus_request_status → Para el polling de la app
//      (¿ya terminó el análisis? ¿cuál fue el resultado?)
//
//   2. sonus_mood_history → Historial personal del usuario
//      (patrones a lo largo del tiempo, para análisis futuro)
//
// El historial de mood podría usarse en el futuro para:
//   - Mostrar "tu semana en música" (como Spotify Wrapped)
//   - Mejorar las sugerencias basándose en patrones
//   - Cambiar el shader del player según la hora del día
// ============================================================
class MoodStorageService {

    private val dynamoClient = DynamoDbClient.builder().build()

    private val requestTable = System.getenv("REQUEST_STATUS_TABLE") ?: "sonus_request_status"
    private val moodHistoryTable = System.getenv("MOOD_HISTORY_TABLE") ?: "sonus_mood_history"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Guarda el resultado del análisis de mood.
     * Actualiza el estado de la solicitud (para polling) y
     * guarda en el historial personal del usuario.
     *
     * @param requestId UUID de la solicitud (para polling)
     * @param userId ID del usuario (actualmente siempre "user_1")
     * @param moodResult El resultado del análisis de DeepSeek
     */
    fun saveMoodResult(requestId: String, userId: String, moodResult: MoodResponse) {
        val resultJson = json.encodeToString(moodResult)

        // 1. Actualizar el estado de la solicitud a COMPLETED
        // La app Android estaba esperando esto en el polling
        updateRequestCompleted(requestId, resultJson)

        // 2. Guardar en el historial personal del usuario
        // Permite análisis de patrones en el futuro
        saveMoodHistory(userId, moodResult)
    }

    /**
     * Actualiza el status de la solicitud a COMPLETED con los datos del mood.
     */
    private fun updateRequestCompleted(requestId: String, resultJson: String) {
        val request = UpdateItemRequest.builder()
            .tableName(requestTable)
            .key(mapOf("pk" to AttributeValue.fromS(requestId)))
            .updateExpression("SET #status = :status, #data = :data")
            .expressionAttributeNames(mapOf(
                "#status" to "status",
                "#data" to "data"
            ))
            .expressionAttributeValues(mapOf(
                ":status" to AttributeValue.fromS("COMPLETED"),
                ":data" to AttributeValue.fromS(resultJson)
            ))
            .build()

        dynamoClient.updateItem(request)
    }

    /**
     * Guarda una entrada en el historial de mood del usuario.
     * Clave de acceso: userId (PK) + timestamp (SK)
     * Esto permite consultar el historial ordenado por tiempo.
     */
    private fun saveMoodHistory(userId: String, moodResult: MoodResponse) {
        val now = System.currentTimeMillis() / 1000  // Epoch en segundos
        val ttl90Days = now + (90 * 24 * 60 * 60)   // TTL: 90 días

        val item = mapOf(
            // PK: userId para buscar todo el historial de un usuario
            "pk" to AttributeValue.fromS(userId),
            // SK: timestamp para ordenar cronológicamente
            "sk" to AttributeValue.fromN(now.toString()),
            "mood" to AttributeValue.fromS(moodResult.mood ?: "unknown"),
            "shaderMood" to AttributeValue.fromS(moodResult.shaderMood ?: "XEROGRAPHIC"),
            "description" to AttributeValue.fromS(moodResult.description ?: ""),
            "createdAt" to AttributeValue.fromN(now.toString()),
            "ttl" to AttributeValue.fromN(ttl90Days.toString())  // Expira en 90 días
        )

        val request = PutItemRequest.builder()
            .tableName(moodHistoryTable)
            .item(item)
            .build()

        dynamoClient.putItem(request)
    }

    /**
     * Marca una solicitud como FAILED cuando el análisis falló.
     */
    fun updateRequestFailed(requestId: String, errorMessage: String) {
        val request = UpdateItemRequest.builder()
            .tableName(requestTable)
            .key(mapOf("pk" to AttributeValue.fromS(requestId)))
            .updateExpression("SET #status = :status, #error = :error")
            .expressionAttributeNames(mapOf(
                "#status" to "status",
                "#error" to "error"
            ))
            .expressionAttributeValues(mapOf(
                ":status" to AttributeValue.fromS("FAILED"),
                ":error" to AttributeValue.fromS(errorMessage)
            ))
            .build()

        dynamoClient.updateItem(request)
    }
}
