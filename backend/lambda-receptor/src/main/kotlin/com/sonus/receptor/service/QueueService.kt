package com.sonus.receptor.service

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

// ============================================================
// QUEUE SERVICE — Operaciones con SQS
// ============================================================
// Este servicio envía mensajes a las colas SQS.
// Cada tipo de solicitud va a una cola diferente:
//
//   lyrics_queue → Lambda Lyrics Processor
//   mood_queue   → Lambda Mood Analyzer
//
// Las URLs de las colas vienen de variables de entorno
// que Terraform configura automáticamente al desplegar.
//
// ¿Por qué SQS y no llamar a la Lambda directamente?
//   1. Desacoplamiento: el Receptor no sabe quién procesa
//   2. Retry automático: si la Lambda falla, SQS reintenta
//   3. Buffer: si hay 1000 requests, SQS los cola sin saturar
// ============================================================
class QueueService {

    // Cliente SQS — se inicializa una vez por instancia de Lambda
    private val sqsClient = SqsClient.builder().build()

    // URLs de las colas — configuradas por Terraform como env vars
    private val lyricsQueueUrl = System.getenv("LYRICS_QUEUE_URL")
        ?: error("LYRICS_QUEUE_URL environment variable not set")
    private val moodQueueUrl = System.getenv("MOOD_QUEUE_URL")
        ?: error("MOOD_QUEUE_URL environment variable not set")

    /**
     * Envía un mensaje a la cola de letras.
     * La Lambda Lyrics Processor consumirá este mensaje.
     *
     * @param messageBody JSON con requestId, artist, title, cacheKey
     */
    fun sendToLyricsQueue(messageBody: String) {
        val request = SendMessageRequest.builder()
            .queueUrl(lyricsQueueUrl)
            .messageBody(messageBody)
            // MessageGroupId no necesario — usamos cola estándar (no FIFO)
            .build()

        sqsClient.sendMessage(request)
    }

    /**
     * Envía un mensaje a la cola de mood.
     * La Lambda Mood Analyzer consumirá este mensaje.
     *
     * @param messageBody JSON con requestId y el payload MoodRequest completo
     */
    fun sendToMoodQueue(messageBody: String) {
        val request = SendMessageRequest.builder()
            .queueUrl(moodQueueUrl)
            .messageBody(messageBody)
            .build()

        sqsClient.sendMessage(request)
    }
}
