# Implementation Plan: Sonus - Bala Trazadora

## Overview

Implementación con metodología Tracer Bullet. Cada bala produce una app funcional de punta a punta, agregando un flujo completo (domain → data → UI) en cada iteración. Stack: Kotlin + Jetpack Compose + Media3 + Room + Hilt + Retrofit + Coil. Arquitectura: Clean Architecture + MVVM (3 módulos: :app, :domain, :data).

## Task Dependency Graph

```json
{
  "waves": [
    [1],
    [2],
    [3],
    [4],
    [5],
    [6],
    [7],
    [8],
    [9],
    [10]
  ]
}
```

## Tasks

- [x] 1. Project Setup — Multi-Module Gradle con Version Catalog
  Proyecto Android "Sonus" (com.sonus.player) con 3 módulos (:app, :domain, :data). Version catalog completo. Hilt, KSP, Media3, Room, Retrofit, Coil configurados. minSdk 29, targetSdk 36, Java 17.
  Requirements: R1, R6, R8 | Design: Stack Tecnológico

- [x] 2. Domain Layer — Entidades, Interfaces y Use Cases
  Todas las entidades (Track, Playlist, PlaybackState, EqPreset, etc.), enums, sealed classes. Interfaces de repositorio y controladores. Use cases completos.
  Requirements: R1, R3, R4, R5 | Design: Domain Models, Components

- [ ] 3. Bala 1A — Room Database + Scanner (datos mínimos para reproducir)
  Crear TrackEntity + TrackDao (insert, getAll, getById, search). Crear SonusDatabase con una sola entity. Implementar MusicScannerImpl: usar MediaStore para descubrir archivos de audio, extraer metadatos básicos (título, artista, álbum, duración, path) con JAudioTagger. Implementar MusicRepositoryImpl que indexa tracks escaneados en Room y expone Flow<List<Track>>. Proveer todo via Hilt @Module. Verificar: el scanner encuentra archivos MP3 en el dispositivo y los guarda en Room.
  Requirements: R1, R6, R8 | Design: Room Schema, Music Scanner

- [ ] 4. Bala 1B — Audio Engine (reproducción básica)
  Implementar Media3PlayerController: play(track), pause(), resume(), next(), previous(), seekTo(). Crear ForegroundService con MediaStyle notification. Emitir StateFlows: playbackState, currentTrack, progress. Adquirir PARTIAL_WAKE_LOCK durante reproducción. Implementar audio focus (pause en llamadas). Proveer via Hilt @Module. Verificar: se puede reproducir un MP3 local, pausar, y la notificación aparece.
  Requirements: R1, R7, R8 | Design: Audio Engine, PlayerController

- [ ] 5. Bala 1C — UI mínima (Library + NowPlaying + permisos)
  Implementar flujo de permisos (READ_MEDIA_AUDIO, rationale, redirect a Settings). Crear LibraryScreen: LazyColumn con TrackListItem (título, artista, duración). Crear NowPlayingScreen: título + artista + seekbar + botones play/pause/next/prev. Crear MiniPlayerBar en bottom. Crear LibraryViewModel y PlayerViewModel conectados a use cases y PlayerController. Configurar Navigation (Library ↔ NowPlaying). Trigger escaneo al primer acceso post-permiso. Verificar: abrir app → dar permiso → ver lista de canciones → tocar una → se reproduce con controles funcionales.
  Requirements: R1, R2, R6 | Design: UI Controller, Screens, Navigation

- [ ] 6. Bala 2 — Carátulas de álbumes (completa)
  Agregar CoverArtCacheEntity + CoverArtDao a Room. Implementar CoverArtResolverImpl con cadena: embedded (JAudioTagger bytes) → cache local → Last.fm API → MusicBrainz API → gradiente generado. Crear LastFmApiService y MusicBrainzApiService (Retrofit, timeout 3s). Implementar generación de gradiente determinista (hash de nombre artista). Integrar Coil en UI: mostrar cover art en NowPlayingScreen (grande) y en TrackListItem/AlbumGrid (thumbnail 64x64). Agregar tab Albums a LibraryScreen (LazyVerticalGrid con carátulas). Implementar cache de 30 días. Verificar: las canciones muestran carátulas (embebidas o descargadas), albums se ven como grid con imágenes.
  Requirements: R3 | Design: Cover Art Resolver, APIs

- [ ] 7. Bala 3 — Letras sincronizadas (completa)
  Agregar LyricsCacheEntity + LyricsDao a Room. Crear LrcLibApiService (Retrofit, endpoint /api/get, sin API key). Crear GeniusApiService (Retrofit, search + scrape, API key gratuita). Implementar LyricsRepositoryImpl con cadena: cache → LRCLIB (sincronizadas) → Genius (texto plano) → NotFound. Implementar parsing de formato .lrc a List<SyncedLine>. Crear LyricsScreen: letras sincronizadas con auto-scroll, highlight de línea actual (#FFD700), font size ajustable. Crear LyricsViewModel. Acceso desde NowPlaying (swipe-up o botón). Implementar fallback a texto plano si no hay sync. Verificar: al reproducir una canción, las letras aparecen y se sincronizan con la música.
  Requirements: R4 | Design: Lyrics Provider, LRCLIB, Genius

- [ ] 8. Bala 4A — Playlists + Historial
  Agregar PlaylistEntity, PlaylistTrackCrossRef, HistoryEntity + DAOs a Room. Implementar PlaylistRepositoryImpl (CRUD, agregar/remover tracks, reordenar). Implementar PlaybackHistoryRepositoryImpl (buffer 100 entries FIFO). Implementar shuffle (Fisher-Yates, no-repeat). Crear PlaylistsScreen (lista de playlists, crear nueva, detalle con tracks). Crear PlaylistViewModel. Agregar "Add to Playlist" como acción en tracks. Implementar pantalla historial (últimas 100, tap para reproducir). Implementar repeat mode (Off/One/All). Verificar: crear playlist, agregar canciones, reproducir desde playlist, ver historial.
  Requirements: R5 | Design: Playlist Repository, History, Shuffle

- [ ] 9. Bala 4B — Ecualizador + Sleep Timer + Settings
  Implementar EqualizerController con android.media.audiofx.Equalizer (5 presets, 5 bandas ±12dB). Implementar DataStore preferences (tema, EQ activo, estado guardado). Implementar sleep timer (countdown, fade-out 3s). Crear SettingsScreen: selector tema (Light/Dark/System), EQ (presets como chips + sliders de bandas), sleep timer (15/30/60/90/custom), limpiar caché. Crear SettingsViewModel. Implementar persistencia de EQ preset entre sesiones. Verificar: cambiar EQ se escucha en la música, sleep timer apaga la reproducción, tema cambia la app.
  Requirements: R2, R5 | Design: EQ, Configuration, Settings

- [ ] 10. Bala 5 — Polish Visual + Persistencia + Error Handling
  Aplicar Material Design 3 customizado: Dynamic Color (Android 12+), paleta restringida (2-3 colores), dark mode optimizado (#121212), tipografía custom. Implementar transiciones CrossFade (<300ms) entre pantallas. Implementar persistencia de estado: guardar posición/track/cola al cerrar, restaurar al abrir. Implementar error handling completo: skip en archivos corruptos, fallback en APIs, snackbar para errores de usuario, retry con backoff. Implementar búsqueda con debounce 300ms. Agregar tab Artists a Library. Implementar Bluetooth/headset button handling. Implementar "becoming noisy" (pause al desconectar auriculares). Accessibility: content descriptions, contraste. Verificar: la app se siente pulida, profesional, y resiliente a errores.
  Requirements: R1, R2, R5, R6, R7 | Design: Theme, Error Handling, Persistence

## Notes

- API keys: Last.fm (gratuita), Genius (gratuita). LRCLIB y MusicBrainz sin key.
- Cada bala produce una app funcional que se puede instalar y usar.
- Tasks 1 y 2 ya completadas (commit en develop).
- La Bala 1 (Tasks 3+4+5) es la más importante: app que reproduce música.
- Después de cada bala, hacer commit y verificar en dispositivo real.
