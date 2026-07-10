# Sonus Backend — Kotlin Lambdas + AWS

Backend serverless de la app Sonus. Arquitectura orientada a eventos (EDA) con AWS Lambda, SQS y DynamoDB.

## Arquitectura

```
App Android
    │
    ▼ HTTP
API Gateway
    │
    ▼
Lambda Receptor (Kotlin)
    │
    ├──► SQS lyrics_queue ──► Lambda Lyrics Processor ──► DeepSeek API
    │                                                           │
    └──► SQS mood_queue   ──► Lambda Mood Analyzer   ──► DeepSeek API
                                                            │
                                                       DynamoDB (cache + historial)
```

## Módulos

| Módulo | Descripción |
|--------|-------------|
| `shared` | Data classes compartidas entre Android y Lambdas (contratos de API) |
| `lambda-receptor` | Entrada HTTP. Valida requests, verifica caché, encola en SQS |
| `lambda-lyrics` | Procesa letras. Lee SQS → llama DeepSeek → guarda en DynamoDB |
| `lambda-mood` | Analiza mood. Lee SQS → llama DeepSeek → guarda historial |

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/lyrics` | Solicitar letra de una canción |
| `POST` | `/mood` | Enviar historial de escucha para análisis |
| `GET` | `/result/{requestId}` | Polling para obtener resultado |

### Ejemplo: Pedir letra

**Request:**
```json
POST /lyrics
{
  "artist": "Bad Bunny",
  "title": "Titi Me Preguntó"
}
```

**Response inmediata (202 — en proceso):**
```json
{
  "requestId": "abc-123",
  "status": "PENDING"
}
```

**Polling con GET /result/abc-123 (cuando termina):**
```json
{
  "requestId": "abc-123",
  "status": "COMPLETED",
  "lyrics": "Ay, vente pa'cá...",
  "confidence": "high",
  "source": "deepseek"
}
```

## Compilar los JARs

Desde la raíz del proyecto:

```bash
cd backend

# Compilar todos los módulos
./gradlew :lambda-receptor:shadowJar
./gradlew :lambda-lyrics:shadowJar
./gradlew :lambda-mood:shadowJar

# Los JARs se generan en:
# lambda-receptor/build/libs/lambda-receptor.jar
# lambda-lyrics/build/libs/lambda-lyrics.jar
# lambda-mood/build/libs/lambda-mood.jar
```

## Variables de entorno requeridas por cada Lambda

### Lambda Receptor
| Variable | Descripción |
|----------|-------------|
| `LYRICS_CACHE_TABLE` | Nombre de la tabla DynamoDB de caché de letras |
| `REQUEST_STATUS_TABLE` | Nombre de la tabla DynamoDB de estados |
| `LYRICS_QUEUE_URL` | URL de la cola SQS de letras |
| `MOOD_QUEUE_URL` | URL de la cola SQS de mood |

### Lambda Lyrics + Lambda Mood
| Variable | Descripción |
|----------|-------------|
| `DEEPSEEK_API_KEY` | API key de DeepSeek (obtener en platform.deepseek.com) |
| `LYRICS_CACHE_TABLE` | Tabla DynamoDB de caché |
| `REQUEST_STATUS_TABLE` | Tabla DynamoDB de estados |
| `MOOD_HISTORY_TABLE` | Tabla DynamoDB de historial (solo Lambda Mood) |

Todas estas variables las configura Terraform automáticamente al desplegar.
