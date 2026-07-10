package com.sonus.receptor.service

import com.sonus.shared.models.RequestResult
import com.sonus.shared.models.RequestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

// ============================================================
// CACHE SERVICE — Operaciones con DynamoDB
// ============================================================
// Este servicio maneja toda la comunicación con DynamoDB.
//
// Tablas que utiliza:
//   sonus_lyrics_cache    → Cache de letras (evita llamar a DeepSeek dos veces)
//   sonus_request_status  → Estado de solicitudes pendientes (para polling)
//
// El DynamoDbClient se crea UNA vez por instancia de Lambda.
// AWS reutiliza la instancia, por lo que el cliente se reutiliza
// y no se crea en cada invocación (mejor performance).
// ============================================================
class CacheService {

    // Cliente de DynamoDB — se inicializa una vez
    // La región se toma de la variable de entorno AWS_REGION
    // que AWS configura automáticamente en Lambda
    private val dynamoClient = DynamoDbClient.builder()
        .build()

    // Nombres de las tablas — deben coincidir con lo definido en Terraform
    private val lyricsTable = System.getenv("LYRICS_CACHE_TABLE") ?: "sonus_lyrics_cache"
    private val requestTable = System.getenv("REQUEST_STATUS_TABLE") ?: "sonus_request_status"

    private val json = Json { ignoreUnknownKeys = true }

    // ============================================================
    // LYRICS CACHE
    // ============================================================

    /**
     * Busca una letra en el caché de DynamoDB.
     *
     * @param cacheKey Clave normalizada: "bad_bunny#titi_me_pregunto"
     * @return El texto de la letra si existe en caché, null si no
     */
    fun getLyricsFromCache(cacheKey: String): String? {
        return try {
            val request = GetItemRequest.builder()
                .tableName(lyricsTable)
                .key(mapOf("pk" to AttributeValue.fromS(cacheKey)))
                .build()

            val result = dynamoClient.getItem(request)

            // Extraer el campo "lyrics" si el item existe
            result.item()?.get("lyrics")?.s()
        } catch (e: Exception) {
            // Si DynamoDB falla, devolver null (el Processor lo generará)
            null
        }
    }

    // ============================================================
    // REQUEST STATUS
    // ============================================================

    /**
     * Guarda el estado inicial de una solicitud como PENDING.
     * Llamado inmediatamente después de encolar en SQS.
     *
     * @param requestId UUID único generado por el Receptor
     * @param status Estado inicial (siempre PENDING en el Receptor)
     */
    fun saveRequestStatus(requestId: String, status: RequestStatus) {
        try {
            val item = mutableMapOf(
                "pk" to AttributeValue.fromS(requestId),
                "status" to AttributeValue.fromS(status.name),
                // TTL de 24 horas — DynamoDB elimina el item automáticamente
                // Esto evita acumulación de items "pending" huérfanos
                "ttl" to AttributeValue.fromN(
                    ((System.currentTimeMillis() / 1000) + 86400).toString()
                )
            )

            val request = PutItemRequest.builder()
                .tableName(requestTable)
                .item(item)
                .build()

            dynamoClient.putItem(request)
        } catch (e: Exception) {
            // Log del error — el cliente verá status PENDING en polling
            println("ERROR saving request status: ${e.message}")
        }
    }

    /**
     * Lee el resultado de una solicitud para el polling del cliente.
     * Retorna null si el requestId no existe.
     *
     * @param requestId El UUID de la solicitud
     * @return RequestResult con el estado y datos, o null si no existe
     */
    fun getRequestResult(requestId: String): RequestResult? {
        return try {
            val request = GetItemRequest.builder()
                .tableName(requestTable)
                .key(mapOf("pk" to AttributeValue.fromS(requestId)))
                .build()

            val result = dynamoClient.getItem(request)

            if (!result.hasItem() || result.item().isEmpty()) return null

            val item = result.item()
            RequestResult(
                requestId = requestId,
                status = RequestStatus.valueOf(item["status"]?.s() ?: "PENDING"),
                data = item["data"]?.s(),
                error = item["error"]?.s()
            )
        } catch (e: Exception) {
            null
        }
    }
}
