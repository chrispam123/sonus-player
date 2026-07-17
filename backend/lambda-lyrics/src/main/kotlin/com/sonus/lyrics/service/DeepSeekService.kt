package com.sonus.lyrics.service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ============================================================
// DEEPSEEK SERVICE — Integración con la API de DeepSeek
// ============================================================
// DeepSeek es el LLM (Large Language Model) que generamos las
// letras de canciones cuando LRCLIB y Genius no las tienen.
//
// API: https://api.deepseek.com/v1/chat/completions
// Modelo: deepseek-chat (el más económico y suficiente para letras)
//
// Costos aproximados:
//   ~$0.001 por letra generada (input + output tokens)
//   Con caché en DynamoDB, solo se genera UNA vez por canción
//
// La API key se guarda en AWS Secrets Manager (no en el código)
// y se pasa como variable de entorno DEEPSEEK_API_KEY.
// ============================================================
class DeepSeekService {

    // API key de DeepSeek — viene de variable de entorno (configurada por Terraform)
    private val apiKey = System.getenv("DEEPSEEK_API_KEY")
        ?: error("DEEPSEEK_API_KEY environment variable not set")

    private val baseUrl = "https://api.deepseek.com/v1/chat/completions"

    // OkHttp client con timeouts generosos para el LLM
    // DeepSeek puede tardar hasta 15-20 segundos en respuestas largas
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)  // 60s para respuestas largas
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Genera la letra completa de una canción usando DeepSeek.
     *
     * Prompt diseñado para maximizar la calidad:
     *   - Especifica que queremos la letra COMPLETA
     *   - Pide que no agregue comentarios ni explicaciones
     *   - Incluye el idioma esperado basado en el artista
     *
     * @param artist Nombre del artista (ya limpio por MetadataCleaner de Android)
     * @param title Título de la canción (ya limpio)
     * @return Texto completo de la letra
     * @throws Exception si DeepSeek falla o la respuesta es inválida
     */
    fun generateLyrics(artist: String, title: String): String {
        // Construir el prompt optimizado para letras
        val prompt = buildPrompt(artist, title)

        // Construir el body del request en formato Chat Completions de OpenAI
        // (DeepSeek usa el mismo formato que OpenAI)
        val requestBody = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a music lyrics database. When asked for song lyrics, search the internet to provide the complete and accurate lyrics without any additional commentary, headers, or explanations. Only output the lyrics text."
                    },
                    {
                        "role": "user",
                        "content": "${prompt.replace("\"", "\\\"")}"
                    }
                ],
                "temperature": 0.3,
                "max_tokens": 2000,
                "enable_search": true
            }
        """.trimIndent()

        // Construir y ejecutar el request HTTP
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("DeepSeek API error: ${response.code} ${response.body?.string()}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("DeepSeek API returned empty response")

        // Extraer el texto de la respuesta JSON de OpenAI/DeepSeek
        // El texto está en: choices[0].message.content
        return extractContentFromResponse(responseBody)
    }

    /**
     * Construye el prompt optimizado para obtener letras completas.
     * El prompt está en inglés (DeepSeek responde mejor en inglés)
     * pero la letra la devuelve en el idioma original de la canción.
     */
    private fun buildPrompt(artist: String, title: String): String =
        "Please provide the complete lyrics for the song '$title' by $artist. " +
                "Output only the lyrics without any introduction, explanation, or commentary."

    /**
     * Extrae el contenido de texto de la respuesta JSON de DeepSeek.
     * La respuesta sigue el formato OpenAI Chat Completions.
     *
     * Formato de la respuesta:
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "AQUÍ ESTÁ LA LETRA..."
     *       }
     *     }
     *   ]
     * }
     */
    private fun extractContentFromResponse(responseJson: String): String {
        // Parsing manual sin librería extra para mantener el JAR ligero
        // Buscamos el patrón "content":"..."
        val contentStart = responseJson.indexOf("\"content\":\"") + 11
        val contentEnd = responseJson.lastIndexOf("\"")

        if (contentStart < 11 || contentEnd <= contentStart) {
            throw Exception("Could not extract content from DeepSeek response")
        }

        return responseJson.substring(contentStart, contentEnd)
            .replace("\\n", "\n")  // Convertir \n escapado a saltos de línea reales
            .replace("\\\"", "\"") // Convertir \" escapado a comillas reales
            .trim()
    }
}
