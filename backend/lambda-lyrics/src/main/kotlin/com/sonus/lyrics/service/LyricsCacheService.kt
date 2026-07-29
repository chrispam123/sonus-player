package com.sonus.lyrics.service

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

// ============================================================
// LYRICS CACHE SERVICE — Persistencia en DynamoDB
// ============================================================
// Este servicio escribe en DynamoDB desde la Lambda de Letras.
// Tiene dos responsabilidades:
//
//   1. Guardar la letra generada en el caché (sonus_lyrics_cache)
//      → Para que futuras solicitudes de la misma canción sean
//        instantáneas (sin llamar a DeepSeek de nuevo)
//
//   2. Actualizar el estado de la solicitud (sonus_request_status)
//      → Para que el cliente que está haciendo polling sepa
//        si la letra ya está lista (COMPLETED) o falló (FAILED)
//
// El TTL (Time To Live) en DynamoDB elimina items automáticamente:
//   - Letras cacheadas: 30 días (suficiente para uso normal)
//   - Estados de request: 24 horas (el polling no dura tanto)
// ============================================================
class LyricsCacheService {

    private val dynamoClient = DynamoDbClient.builder().build()

    // Nombres de tablas — vienen de variables de entorno configuradas por Terraform
    private val lyricsTable = System.getenv("LYRICS_CACHE_TABLE") ?: "sonus_lyrics_cache"
    private val requestTable = System.getenv("REQUEST_STATUS_TABLE") ?: "sonus_request_status"

    /**
     * Guarda la letra generada en el caché de DynamoDB.
     * Si la clave ya existe, la sobreescribe (onConflict = replace).
     *
     * @param cacheKey Clave normalizada: "bad_bunny#titi_me_pregunto"
     * @param lyrics Texto completo de la letra generada por DeepSeek
     */
    fun saveLyrics(cacheKey: String, lyrics: String) {
        // TTL = ahora + 30 días (en segundos para DynamoDB)
        val ttl30Days = (System.currentTimeMillis() / 1000) + (30 * 24 * 60 * 60)

        val item = mapOf(
            "pk" to AttributeValue.fromS(cacheKey),    // Clave primaria
            "lyrics" to AttributeValue.fromS(lyrics),   // Texto de la letra
            "source" to AttributeValue.fromS("deepseek"), // Origen del dato
            "createdAt" to AttributeValue.fromN(
                (System.currentTimeMillis() / 1000).toString()
            ),
            "ttl" to AttributeValue.fromN(ttl30Days.toString()) // Expiración automática
        )

        val request = PutItemRequest.builder()
            .tableName(lyricsTable)
            .item(item)
            .build()

        dynamoClient.putItem(request)
    }

    /**
     * Actualiza el estado de una solicitud en DynamoDB.
     * Llamado después de procesar exitosamente o al fallar.
     *
     * @param requestId UUID de la solicitud (el cliente hace polling con este ID)
     * @param status "COMPLETED" o "FAILED"
     * @param data La letra (solo cuando status = COMPLETED)
     * @param error Mensaje de error (solo cuando status = FAILED)
     */
    fun updateRequestStatus(
        requestId: String,
        status: String,
        data: String? = null,
        error: String? = null
    ) {
        // Construir la expresión de actualización dinámicamente
        // Solo actualizamos los campos que tienen valor
        val updateExpression = StringBuilder("SET #status = :status")
        val expressionNames = mutableMapOf("#status" to "status")
        val expressionValues = mutableMapOf(
            ":status" to AttributeValue.fromS(status)
        )

        if (data != null) {
            updateExpression.append(", #data = :data")
            expressionNames["#data"] = "data"
            expressionValues[":data"] = AttributeValue.fromS(data)
        }

        if (error != null) {
            updateExpression.append(", #error = :error")
            expressionNames["#error"] = "error"
            expressionValues[":error"] = AttributeValue.fromS(error)
        }

        val request = UpdateItemRequest.builder()
            .tableName(requestTable)
            .key(mapOf("pk" to AttributeValue.fromS(requestId)))
            .updateExpression(updateExpression.toString())
            .expressionAttributeNames(expressionNames)
            .expressionAttributeValues(expressionValues)
            .build()

        dynamoClient.updateItem(request)
    }
}
