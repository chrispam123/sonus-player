# Sonus — Especificación del Producto (SPEC) AYUDADO CON IA en la elaboracion pero el pensamiento sistemico las decision de arquitectura, patrones de diseño, la seguridad y la responsabilidad de las decisiones es 100% humana.

**Versión:** 1.0.6 | **Fecha:** Agosto 2026 | **Plataforma:** Android 10+ (API 29)

---

## 1. Visión del Producto

Sonus es un reproductor de música Android con inteligencia artificial que analiza tu estado de ánimo basándose en lo que escuchas. Combina música local, streaming de música Creative Commons (ccMixter), generación de letras con IA (DeepSeek)y recomendación de canciones, y visualizadores GLSL en tiempo real(graficos a ritmo de la música).

### 1.1 Propuesta de valor

| Funcionalidad | ¿Qué hace? | ¿Por qué es único? |
|---|---|---|
| **Reproducción local** | Escanea MediaStore y reproduce MP3/FLAC/WAV | Base de cualquier reproductor |
| **Streaming ccMixter** | Busca y reproduce música Creative Commons | Sin límites, sin royalties |
| **Letras con IA** | LRCLIB → Genius → DeepSeek (fallback) | Letras para CUALQUIER canción |
| **Mood IA** | Analiza historial de escucha cada 15 min | El shader cambia de color según tu estado de ánimo |y ofrece recomendación de canciones
| **Living Canvas** | Shaders GLSL xerográficos estilo Munari | Visualizador único, no genérico |

### 1.2 Público objetivo

Usuarios de Android que escuchan música local, quieren letras para cualquier canción (incluso las oscuras/independientes), y disfrutar de una experiencia visual que reacciona a la música y a su estado emocional.Resolví un problema porque no habian alternativas gratis y sin publicidad, y porque queria aplicar mi filosofía de diseño y la empatia músical ayudado por el analisis de la Inteligencia Artificial.

---

## 2. Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    APP ANDROID (Kotlin)                      │
│  app/          domain/          data/                        │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────────┐ │
│  │ Compose  │  │ Casos Uso  │  │ Room (DB local)          │ │
│  │ UI       │  │ Interfaces │  │ ccMixter API (Retrofit)  │ │
│  │ Shaders  │  │ Modelos    │  │ Sonus API (Retrofit)     │ │
│  │ MVVM     │  └────────────┘  │ Media3 ExoPlayer         │ │
│  └──────────┘                  └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ HTTPS
┌─────────────────────────────────────────────────────────────┐
│                    AWS (us-east-1)                           │
│                                                             │
│  API Gateway (HTTP v2)                                      │
│    ├── POST /lyrics ──────► Lambda Receptor                 │
│    ├── POST /mood   ──────►         │                       │
│    └── GET  /result  ──────►        │                       │
│                                       │                     │
│                        ┌──────────────┼──────────────┐      │
│                        ▼              ▼              ▼      │
│                    SQS lyrics/DLQ    SQS mood/DLQ      DynamoDB ×3  │
│                        │              │           (cache,   │
│                        ▼              ▼           status,   │
│                    Lambda Lyrics  Lambda Mood    history)   │
│                        │              │                     │
│                        └──────┬───────┘                     │
│                               ▼                             │
│                        DeepSeek API                          │
│                   (chat + enable_search)                     │
└─────────────────────────────────────────────────────────────┘
```

### 2.1 Patrones

- **Clean Architecture:** `app` → `domain` → `data`
- **MVVM** con `HiltViewModel` compartido entre pantallas
- **EDA** (Event-Driven): API Gateway → SQS → Lambda → DynamoDB
- **Polling asíncrono:** POST → 202 → GET /result cada 2s hasta COMPLETED

---

## 3. Infraestructura

### 3.1 Terraform

| Archivo | Responsabilidad |
|---------|----------------|
| `main.tf` | Provider AWS + locals (prefijo `sonus-{workspace}-`) |
| `backend.tf` | Estado remoto S3 + `use_lockfile = true` (TF ≥1.10) |
| `lambda.tf` | 3 funciones Lambda (Java 17, shadow JAR) |
| `api_gateway.tf` | HTTP API v2, rutas, CORS, CloudWatch logs |
| `dynamodb.tf` | 3 tablas PAY_PER_REQUEST con TTL |
| `sqs.tf` | 2 colas + 2 DLQ, visibilidad 300s, retención 24h |
| `iam.tf` | Roles mínimo privilegio por Lambda |
| `variables.tf` | `environment`, `deepseek_api_key`, memory, timeout |
| `outputs.tf` | `api_url` + endpoints |

### 3.2 Recursos AWS

| Recurso | Cantidad | Nombres |
|---------|:--------:|---------|
| API Gateway HTTP v2 | 1 | `sonus-{env}-apigateway` |
| Lambda (Java 17, 512MB) | 3 | receptor, lyrics-processor, mood-analyzer |
| SQS Standard | 2 | lyrics-queue, mood-queue |
| SQS DLQ | 2 | lyrics-dlq, mood-dlq |
| DynamoDB PAY_PER_REQUEST | 3 | lyrics-cache, request-status, mood-history |
| IAM Roles | 3 | uno por Lambda |
| CloudWatch Logs | 1 | API Gateway |
| API Key (random_password) | 1 | `sonus-{env}-app-key` |

### 3.3 Seguridad

| Capa | Implementación | Dónde |
|:----:|---------------|-------|
| **Autenticación CI** | OIDC (`id-token: write`) + roles IAM temporales (1h) | `ci.yml`, `ci-prod.yml` |
| **Mínimo privilegio CI** | Roles scoped por nombre de recurso (`sonus-{env}-*`) | CLI `aws iam put-role-policy` |
| **Separación entornos** | Dev solo puede `sonus-develop-*`, prod solo `sonus-prod-*` | Trust policies OIDC + IAM policies |
| **API Key** | Lambda Receptor valida `x-api-key` contra `SONUS_API_KEY` env var | `Handler.kt`, `lambda.tf` |
| **Throttling** | 10 burst, 5 req/s en API Gateway stage | `api_gateway.tf` (`default_route_settings`) |
| **Aprobación manual** | GitHub Environment `production` con reviewer obligatorio | `ci-prod.yml` |
| **Key aleatoria** | `random_password` 32 chars, rotable vía Terraform | `api_gateway.tf`, `outputs.tf` |

### 3.3 Entornos

| | Develop | Prod |
|---|---|---|
| **Rama Git** | `develop` | `main` |
| **Workspace TF** | `develop` | `prod` |
| **URL API** | `sz4aqbavm2.../develop` | `gsvb2col0d.../prod` |
| **APK flavor** | `develop` | `prod` |
| **Deploy trigger** | Push a develop | Merge develop→main |

### 3.4 CI/CD

| Workflow | Rama | Entorno | Qué hace | Aprobación |
|----------|------|---------|----------|:----------:|
| `ci.yml` | `develop` | `sonus-develop-*` | Build APK develop + JARs → terraform plan+apply | Automático |
| `ci-prod.yml` | `main` | `sonus-prod-*` | Build APK prod + JARs → terraform plan → **aprobación manual** → apply | Manual (`environment: production`) |

---

## 4. Backend

### 4.1 Lambda Receptor

- **Handler:** `APIGatewayV2HTTPEvent → APIGatewayV2HTTPResponse`
- **Rutas:** `POST /lyrics`, `POST /mood`, `GET /result/{requestId}`
- **Stage stripping:** `removePrefix("/$stage")` para HTTP API v2
- **API Key:** Valida `x-api-key` contra `SONUS_API_KEY` env var antes de enrutar
- **Flujo:** validar API Key → validar body → verificar caché DynamoDB → encolar SQS → 202

### 4.2 Lambda Lyrics Processor

- **Trigger:** SQS `lyrics_queue` (batch_size=5)
- **Flujo:** leer mensaje → DeepSeek API (`deepseek-chat` + `enable_search: true`) → guardar en DynamoDB (cache + status)
- **Prompt:** `"Please provide the complete lyrics for the song '$title' by $artist"`

### 4.3 Lambda Mood Analyzer

- **Trigger:** SQS `mood_queue` (batch_size=1)
- **Flujo:** leer mensaje → DeepSeek API con prompt de análisis → guardar en DynamoDB (status + history)
- **Prompt:** Incluye género del artista para mejor detección:
  - Reggaeton/Latin → festive/energetic
  - Folk/acoustic → introspective/melancholic
  - Rock/metal → tense/energetic
  - Ballads/pop → romantic/nostalgic
- **Sugerencias:** Pide 3 canciones NUEVAS de artistas SIMILARES, sin repetir

### 4.4 DynamoDB

| Tabla | PK | SK | TTL | Contenido |
|-------|----|----|-----|-----------|
| `lyrics-cache` | `artist#title` | — | 30 días | lyrics, source, createdAt |
| `request-status` | `requestId` (UUID) | — | 24 horas | status, data, error |
| `mood-history` | `userId` | `timestamp` | 90 días | mood, shaderMood, description |

---

## 5. App Android

### 5.1 UI y Navegación

```
BottomBar: Player | Librería | Listas | Sistema
Pantallas:  NowPlaying, Library, Playlists, Settings
FullScreen: Lyrics, Search, PlaylistDetail, MoodDetail
```

### 5.2 Living Canvas (Shader GLSL)

- **Render:** `TextureView` + EGL manual (no GLSurfaceView para evitar ghost frames)
- **Hilo GL:** `CoroutineScope(Dispatchers.Default)` → 60fps
- **Textura:** Imagen Munari 1967 cargada desde `R.drawable.munari`
- **Efecto:** Distorsión por audio (FFT: graves mueven, agudos ondulan)
- **Moods:** MOIRE_FLOW, RADIAL_WAVE, DIAMOND_GRID, INTERFERENCE, XEROGRAPHIC

### 5.3 Streaming ccMixter

- **API:** `https://ccmixter.org/api/query?tags=X&f=json`
- **Networking:** OkHttp (10s connect, 15s read) via Retrofit
- **Playback:** ExoPlayer Media3 con `DefaultHttpDataSource`
- **Precarga:** `preloadQueue()` — prepara el buffer en background al recibir resultados

### 5.4 Letras

| Fuente | Prioridad | Tipo | Tiempo |
|--------|:---------:|------|:------:|
| LRCLIB | 1ª | Synced (timestamp) | ~200ms |
| Genius | 2ª | Plain text | ~500ms |
| Sonus IA (DeepSeek) | 3ª | AI-generated | 5-60s |

### 5.5 Mood

- **Timer:** Cada 15 minutos
- **Datasource:** Room `PlaybackHistoryRepository` (última hora)
- **Mínimo:** ≥3 canciones para analizar
- **ClearHistory:** Después de cada análisis exitoso (evita acumular repetidos)
- **UI:** 🔮 GlowCircle animado → MoodDetailScreen (color según mood, descripción, links)
- **Reaparición:** Solo cuando el mood cambia respecto al último visto

### 5.6 Build Flavors

| Flavor | URL Backend | versionName |
|--------|------------|-------------|
| `develop` | `sz4aqbavm2.../develop` | `1.0.1-dev` |
| `prod` | `gsvb2col0d.../prod` | `1.0.1` |

---

## 6. UX Flows

### 6.1 Búsqueda y reproducción ccMixter

```
SearchScreen → SearchViewModel.search("jazz")
  → ccMixter API → 20 resultados
  → preloadQueue() (precarga buffer en background)
  → Usuario toca → playTrack() → instantáneo (~100ms)
```

### 6.2 Análisis de Mood

```
Usuario escucha 3+ canciones (local o ccMixter)
  → PlaybackHistory registra automáticamente
  → Timer 15min: requestMoodAnalysis()
  → POST /mood → PENDING → polling 2s → COMPLETED
  → 🔮 GlowCircle aparece (color según mood)
  → Usuario toca → MoodDetailScreen
  → 🔮 desaparece hasta próximo mood distinto
```

### 6.3 Letras con IA

```
NowPlaying → LETRAS → LyricsScreen
  → LRCLIB (synced) → ✅
  → Genius (plain) → ✅
  → Sonus IA → POST /lyrics → polling → letra generada
```

---

## 7. Decisiones Técnicas

| Decisión | Alternativa rechazada | Razón |
|----------|----------------------|-------|
| **TextureView** (no GLSurfaceView) | GLSurfaceView | GLSurfaceView deja ghost frames 4s en SurfaceFlinger |
| **3 tablas DynamoDB** (no 1) | Single-table design | App pública → aislamiento de cargas y monitoreo independiente |
| **HTTP API v2** (no REST v1) | REST API v1 | 70% más barato, menor latencia |
| **DynamoDB locking** (no tabla separada) | DynamoDB table para TF lock | `use_lockfile = true` (S3 nativo, TF ≥1.10) |
| **Workspaces TF** (no keys) | Keys separadas en backend | Mismo código, estados aislados, más simple |
| **enable_search = true** | Sin search | DeepSeek busca en internet → letras reales, no alucinadas |
| **clearHistory() post-análisis** | Historial acumulativo | Evita enviar las mismas canciones repetidamente |
| **API Key en Lambda Receptor** | API Gateway API Keys | HTTP API v2 no soporta API Keys nativas → se valida en el handler |
| **OIDC + roles scoped** | Credenciales estáticas | Sin secretos long-lived, mínimo privilegio por entorno |

---

## 8. Bugs Resueltos (Histórico)

| # | Bug | Causa | Fix |
|---|-----|-------|-----|
| 1 | Shader fantasma 4s al salir | GLSurfaceView en SurfaceFlinger | TextureView + EGL manual |
| 2 | Shader negro | eglMakeCurrent en main thread | Mover a hilo GL en corrutina |
| 3 | Timeout ccMixter 3s | OkHttp readTimeout=3s | 10s/15s |
| 4 | Streaming 8s al seleccionar | DefaultHttpDataSource timeout | preloadQueue() |
| 5 | NavBar no respondía 1er toque | restoreState=true en navigate() | Quitar restoreState |
| 6 | Lambda v1→v2 HTTP API | APIGatewayProxyRequestEvent | APIGatewayV2HTTPEvent |
| 7 | Stage en path | /develop/lyrics → no match | removePrefix("/$stage") |
| 8 | handleResultQuery no quitaba stage | Path sin limpiar en polling | Idem fix |
| 9 | Links truncados | substringBefore("\"}") cortaba JSON | Parseo JSON completo de DeepSeek |
| 10 | ccMixter no en historial | TrackEntity sin streamUrl | Añadir columna + migración Room v5 |
| 11 | ccMixter no en tabla tracks | JOIN con tracks fallaba | trackDao.insertAll() antes del history insert |
| 12 | lastTrackedTrackId no reseteaba | clearHistory sin reset | lastTrackedTrackId = -1 |
| 13 | HTTP cleartext bloqueado en release | Solo ccmixer.org whitelisted | base-config cleartext para todos |
| 14 | Albumes/Artistas no cliqueables | Sin combinedClickable | Añadido en AlbumList y ArtistList |
| 15 | Scanner solo 1ª vez | if (count == 0) | Quitar condicional |
| 16 | CI plan wrong workspace | Faltaba workspace select en plan | Añadido terraform-plan |
| 17 | Doble environment en Lambda TF | 2 bloques environment | Unificado |
| 18 | develop vs dev en variables TF | Validación inconsistente | Alineado a "develop" |
| 19 | access_log_settings sin format | Campo requerido por AWS | JSON format |
| 20 | MoodResponse placeholder "analyzed" | No parseaba JSON de DynamoDB | gson.fromJson() |
| 21 | DeepSeek API Key en historial git | `sk-f9b5eb...` en develop.tfvars | git filter-repo + rotar key |
| 22 | CI credenciales estáticas admin | AWS_ACCESS_KEY_ID permanente | OIDC + roles scoped temporales |

---

## 9. Roadmap

### ✅ Completado

- [x] MVP: reproducción local + streaming ccMixter
- [x] Letras: LRCLIB + Genius + DeepSeek IA
- [x] Mood: análisis de estado de ánimo con IA
- [x] Shaders GLSL con textura Munari
- [x] CI/CD: develop + prod con flavors
- [x] Infra: Terraform con workspaces
- [x] Seguridad: OIDC + API Key + Throttling + mínimo privilegio IAM

### 📋 Pendientes (priorizados)

| Prioridad | Feature | Esfuerzo |
|:---------:|---------|:--------:|
| P0 | Google Play Console — publicar APK | 1 día |
| P1 | Sleep Timer | 2 horas |
| P1 | Equalizer UI (ya existe lógica) | 3 horas |
| P2 | Playlist reorder (drag & drop) | 4 horas |
| P2 | Cover art desde Last.fm | 2 horas |
| P3 | Modo offline (descargar ccMixter) | 1 día |
| P3 | Compartir mood en redes | 3 horas |
| P4 | Chromecast / Android Auto | 2 días |

---




