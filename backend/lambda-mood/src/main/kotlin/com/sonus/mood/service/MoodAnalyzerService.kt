package com.sonus.mood.service

import com.sonus.shared.models.MoodRequest
import com.sonus.shared.models.MoodResponse
import com.sonus.shared.models.RequestStatus
import com.sonus.shared.models.YoutubeLink
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ============================================================
// MOOD ANALYZER SERVICE — Análisis de estado de ánimo con DeepSeek
// ============================================================
// Recibe el historial de canciones de la última hora y construye
// un prompt estructurado para que DeepSeek devuelva:
//
//   1. mood: etiqueta del estado ("melancholy", "energetic", etc.)
//   2. shaderMood: qué patrón visual del Living Canvas mostrar
//      Mapeado a los 5 moods del ShaderRenderer en Android
//   3. description: texto en español para mostrar al usuario
//   4. youtubeLinks: 3-5 sugerencias de artistas similares
//
// El prompt le pide a DeepSeek que responda en JSON estructurado
// para facilitar el parsing de la respuesta.
// ============================================================
class MoodAnalyzerService {

    private val apiKey = System.getenv("DEEPSEEK_API_KEY")
        ?: error("DEEPSEEK_API_KEY environment variable not set")

    private val baseUrl = "https://api.deepseek.com/v1/chat/completions"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Analiza el historial de canciones y determina el mood del usuario.
     *
     * @param request Historial de canciones de la última hora
     * @return MoodResponse con mood, shader, descripción y links
     */
    fun analyzeMood(request: MoodRequest): MoodResponse {
        val prompt = buildMoodPrompt(request)

        val requestBody = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a music psychologist that analyzes listening patterns to determine emotional states. Always respond with valid JSON only, no other text."
                    },
                    {
                        "role": "user",
                        "content": ${JsonPrimitive(prompt)}
                    }
                ],
                "temperature": 0.7,
                "max_tokens": 800
            }
        """.trimIndent()

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            throw Exception("DeepSeek API error: ${response.code}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from DeepSeek")

        return parseMoodResponse(responseBody, request)
    }

    /**
     * Construye el prompt para el análisis de mood.
     * El prompt es estructurado para obtener un JSON parseble.
     *
     * Ejemplo de prompt generado:
     * "Analyze these songs listened in the last hour:
     *  1. "Creep" by Radiohead
     *  2. "Summertime Sadness" by Lana Del Rey
     *  ...
     *  Return a JSON object with these exact fields: ..."
     */
    private fun buildMoodPrompt(request: MoodRequest): String {
        // Construir lista de canciones
        val tracksList = request.tracks
            .take(15) // Máximo 15 canciones para no exceder el contexto
            .mapIndexed { i, track ->
                "${i + 1}. \"${track.title}\" by ${track.artist}"
            }
            .joinToString("\n")

        // Los 5 shader moods disponibles en la app Android
        // DeepSeek debe elegir uno basado en el análisis
        val shaderMoods = """
            - MOIRE_FLOW: slow flowing waves, introspective, melancholy
            - RADIAL_WAVE: expanding circles, emotional depth, nostalgia
            - DIAMOND_GRID: sharp grid, tension, focus, anxiety
            - INTERFERENCE: overlapping patterns, complex emotions, confusion
            - XEROGRAPHIC: all combined, dynamic, mixed emotions
        """.trimIndent()

        return """
            Analyze the emotional state of someone who listened to these songs in the last hour:
            
            $tracksList
            
            Based on this listening pattern, respond with ONLY a JSON object (no markdown, no explanations):
            {
                "mood": "one word emotional state in English (melancholy/energetic/calm/nostalgic/euphoric/tense/focused/romantic)",
                "shaderMood": "choose the best match from: MOIRE_FLOW, RADIAL_WAVE, DIAMOND_GRID, INTERFERENCE, XEROGRAPHIC",
                "description": "2-3 sentences in Spanish describing the emotional state, second person (tú)",
                "youtubeLinks": [
                    {"artist": "Artist Name", "songTitle": "Song Title", "url": "https://www.youtube.com/results?search_query=Artist+Name+Song+Title"},
                    {"artist": "Artist Name 2", "songTitle": "Song Title 2", "url": "https://www.youtube.com/results?search_query=Artist+Name+2+Song+Title+2"},
                    {"artist": "Artist Name 3", "songTitle": "Song Title 3", "url": "https://www.youtube.com/results?search_query=Artist+Name+3+Song+Title+3"}
                ]
            }
            
            Shader mood guide: $shaderMoods
        """.trimIndent()
    }

    /**
     * Parsea la respuesta JSON de DeepSeek al modelo MoodResponse.
     * DeepSeek devuelve el JSON como string dentro del campo "content".
     */
    private fun parseMoodResponse(deepSeekResponse: String, request: MoodRequest): MoodResponse {
        // Extraer el content de la respuesta de DeepSeek
        val contentStart = deepSeekResponse.indexOf("\"content\":\"") + 11
        val rawContent = if (contentStart > 10) {
            deepSeekResponse.substring(contentStart)
                .substringBefore("\"}")
                .replace("\\n", "")
                .replace("\\\"", "\"")
                .trim()
        } else {
            throw Exception("Cannot extract content from DeepSeek response")
        }

        // Parsear el JSON del mood
        return try {
            val moodJson = json.parseToJsonElement(rawContent).jsonObject

            val youtubeLinks = moodJson["youtubeLinks"]?.let { linksElement ->
                try {
                    json.decodeFromJsonElement(
                        ListSerializer(YoutubeLink.serializer()),
                        linksElement
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            MoodResponse(
                requestId = "",  // Se asigna en el Handler
                status = RequestStatus.COMPLETED,
                mood = moodJson["mood"]?.jsonPrimitive?.content,
                shaderMood = moodJson["shaderMood"]?.jsonPrimitive?.content,
                description = moodJson["description"]?.jsonPrimitive?.content,
                youtubeLinks = youtubeLinks
            )
        } catch (e: Exception) {
            // Si el parsing falla, devolver un mood genérico
            MoodResponse(
                requestId = "",
                status = RequestStatus.COMPLETED,
                mood = "calm",
                shaderMood = "XEROGRAPHIC",
                description = "Has estado escuchando música variada esta hora.",
                youtubeLinks = emptyList()
            )
        }
    }
}
