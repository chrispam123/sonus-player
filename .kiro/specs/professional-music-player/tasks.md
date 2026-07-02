# Implementation Plan: Sonus - Professional Music Player for Android

## Overview

Plan de implementación para Sonus (com.sonus.player), un reproductor de música Android profesional. Stack: Kotlin + Jetpack Compose + Media3 + Room + Hilt + Retrofit + Coil. Arquitectura: Clean Architecture + MVVM con 3 módulos (:app, :domain, :data). Carátulas via Last.fm/MusicBrainz. Letras sincronizadas via LRCLIB + Genius (fallback). Material Design 3 customizado. minSdk 29, targetSdk 34.

## Task Dependency Graph

```json
{
  "waves": [
    [1],
    [2],
    [3, 4],
    [5, 6, 7],
    [8, 9, 10],
    [11, 12, 13],
    [14, 15],
    [16]
  ]
}
```

## Tasks

- [ ] 1. Project Setup — Multi-Module Gradle con Version Catalog
  Crear proyecto Android "Sonus" con package `com.sonus.player`. Configurar `settings.gradle.kts` con 3 módulos: `:app`, `:domain`, `:data`. Crear `libs.versions.toml` con versiones pinned: Kotlin, Compose BOM, Material3, Media3, Room, Hilt, Retrofit, OkHttp, Coil, Coroutines, Compose Navigation, JUnit5, Kotest, MockK, Turbine. Configurar `:domain` como módulo Kotlin puro (sin dependencias Android). Configurar `:data` con Room, Retrofit, OkHttp, Coil, Media3, JAudioTagger. Configurar `:app` con Compose, Hilt, Navigation. Setup `@HiltAndroidApp` en Application class. Configurar `AndroidManifest.xml`: minSdk 29, targetSdk 34, permisos READ_MEDIA_AUDIO, INTERNET, WAKE_LOCK, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK.
  Requirements: R1, R6, R8 | Design: Stack Tecnológico, Estructura de Módulos

- [ ] 2. Domain Layer — Entidades, Interfaces y Use Cases
  Crear todas las entidades en `:domain`: Track, Playlist, PlaylistWithTracks, HistoryEntry, PlaybackState, PlaybackProgress, SavedPlaybackState, EqPreset, SyncedLine. Crear enums: AudioFormat, RepeatMode, ThemeMode. Crear sealed classes: CoverArtResult (Embedded/Remote/Generated/NotFound), LyricsResult (Found/NotFound/Error), SyncedLyricsResult (Found/NotFound/Error), Result<T> (Success/Error). Definir interfaces de repositorio: MusicRepository, PlaylistRepository, CoverArtRepository, LyricsRepository, PlaybackHistoryRepository, PreferencesRepository, MusicScannerRepository. Definir interfaces de controladores: PlayerController, EqualizerController. Crear use cases: GetAllTracksUseCase, SearchTracksUseCase, GetCoverArtUseCase, GetLyricsUseCase, GetSyncedLyricsUseCase, ManagePlaylistUseCase, GetPlaybackHistoryUseCase, ScanMusicLibraryUseCase, SavePlaybackStateUseCase, RestorePlaybackStateUseCase.
  Requirements: R1, R3, R4, R5 | Design: Domain Models, Components and Interfaces

- [ ] 3. Data Layer — Room Database, Entities y DAOs
  Crear `SonusDatabase` con @Database(version=1). Crear entities: TrackEntity, PlaylistEntity, PlaylistTrackCrossRef (con foreign keys), HistoryEntity, LyricsCacheEntity, CoverArtCacheEntity. Crear DAOs: TrackDao (getAllTracks, getById, searchTracks, getByAlbum, getByArtist, insert, delete), PlaylistDao (getAll, create, delete, getPlaylistWithTracks, addTrack, removeTrack, reorder), HistoryDao (getRecent con limit 100, insert, clear, deleteOldest), LyricsDao (getByTrackId, insert, clear), CoverArtDao (getByAlbumId, insert, clear). Crear mappers: TrackEntity↔Track, PlaylistEntity↔Playlist, HistoryEntity↔HistoryEntry. Proveer SonusDatabase via Hilt @Module.
  Requirements: R1, R5, R6 | Design: Room Database Schema

- [ ] 4. Data Layer — DataStore Preferences + Hilt Modules
  Implementar DataStore Preferences con keys: THEME_MODE, ACTIVE_EQ_PRESET, EQ_ENABLED, LAST_TRACK_ID, LAST_POSITION_MS, LAST_QUEUE_JSON, LAST_QUEUE_INDEX, SHUFFLE_ENABLED, REPEAT_MODE, EXCLUDED_FOLDERS. Implementar PreferencesRepositoryImpl que lee/escribe estas preferences. Implementar serialización de SavedPlaybackState (JSON en DataStore). Crear Hilt @Module que provee: SonusDatabase, todos los DAOs, DataStore, PreferencesRepository. Verificar que `:data` expone las implementaciones correctamente a `:app` via Hilt.
  Requirements: R2, R5 | Design: DataStore Preferences, Configuration

- [ ] 5. Data Layer — Audio Engine (Media3 ExoPlayer)
  Implementar Media3PlayerController que implementa PlayerController interface del :domain. Configurar ExoPlayer con soporte para MP3, AAC, FLAC, OGG, WAV. Crear MediaSession para controles externos (notificación, Bluetooth, auriculares). Implementar ForegroundService con MediaStyle notification para reproducción en background. Emitir StateFlows: playbackState, currentTrack, progress (actualización cada 100ms). Implementar: play, pause, resume, seekTo, next, previous. Implementar queue management (setPlaylist, currentIndex). Adquirir PARTIAL_WAKE_LOCK solo durante reproducción activa. Implementar audio focus (pause en llamadas, duck en notificaciones). Implementar EqualizerController con android.media.audiofx.Equalizer (5 presets: Rock, Pop, Classical, Jazz, Flat). Proveer via Hilt @Module.
  Requirements: R1, R5, R7, R8 | Design: Audio Engine, PlayerController

- [ ] 6. Data Layer — Music Scanner + Metadata Extractor
  Implementar MusicScannerImpl usando ContentResolver + MediaStore.Audio para descubrimiento de archivos. Implementar MetadataExtractor wrapper sobre JAudioTagger: leer ID3v2 (MP3), Vorbis Comments (OGG/FLAC), APE Tags. Extraer: título, artista, álbum, año, género, track number, duración, bitrate, sample rate, formato. Extraer cover art embebido (bytes). Implementar escaneo en Dispatchers.IO con progress reporting. Implementar filtro de directorios excluidos (lee excluded_folders de DataStore). Implementar detección de archivos corruptos (skip + log, nunca interrumpir). Implementar MusicRepositoryImpl que combina scanner + Room (indexar resultados en TrackEntity). Proveer via Hilt @Module.
  Requirements: R1, R3, R6, R8 | Design: Music Scanner, Metadata Extractor

- [ ] 7. Data Layer — Cover Art Resolver + Lyrics Provider (APIs)
  Implementar CoverArtResolverImpl con cadena: Embedded → Cache → Last.fm → MusicBrainz → Generated gradient. Crear LastFmApiService (Retrofit): endpoint album.getinfo, timeout 3s. Crear MusicBrainzApiService (Retrofit): release search + Cover Art Archive, timeout 3s. Implementar selección por mayor resolución. Implementar gradiente generado (deterministic hash del nombre artista). Implementar cache en Room (CoverArtCacheEntity, 30 días). Implementar LyricsRepositoryImpl con cadena: Cache → LRCLIB → Genius → NotFound. Crear LrcLibApiService (Retrofit): endpoint /api/get, sin API key, timeout 3s. Crear GeniusApiService (Retrofit): search endpoint con API key gratuita, timeout 3s. Implementar parsing de formato .lrc a List<SyncedLine>. Implementar cache en Room (LyricsCacheEntity). Configurar OkHttp con retry (3 intentos, backoff exponencial). Proveer todo via Hilt @Module.
  Requirements: R3, R4 | Design: Cover Art Resolver, Lyrics Provider

- [ ] 8. Data Layer — Playlist Manager + History + Sleep Timer
  Implementar PlaylistRepositoryImpl: crear/eliminar playlists, agregar/remover tracks, reordenar (update position column). Implementar PlaybackHistoryRepositoryImpl con buffer de 100 entradas (FIFO eviction: borrar más antiguo al insertar #101). Implementar shuffle: Fisher-Yates con garantía de no repetición hasta agotar la lista. Implementar sleep timer en PlayerController: countdown, fade-out de 3 segundos antes de pausar. Proveer via Hilt @Module.
  Requirements: R5 | Design: Playlist Repository, History, Shuffle

- [ ] 9. App Layer — Theme, Design System y Navegación
  Crear AppTheme composable con Material Design 3 customizado: max 2-3 colores primarios, Dynamic Color (Android 12+), contraste ≥4.5:1 (≥7:1 dark). Implementar light theme, dark theme (background #121212, surface #1E1E1E), y System mode. Crear tipografía y shapes custom. Crear componentes reutilizables: TrackListItem, AlbumCard, MiniPlayerBar. Configurar Compose Navigation con NavHost y 5 rutas: Library, NowPlaying, Lyrics, Playlists, Settings. Implementar BottomNavigationBar. Implementar transiciones CrossFade (<300ms).
  Requirements: R2 | Design: UI_Controller, Material 3 customizado

- [ ] 10. App Layer — Permission Flow (Storage Manager)
  Implementar flujo de permisos: detectar API level (29-32: READ_EXTERNAL_STORAGE, 33+: READ_MEDIA_AUDIO). Solicitar permiso solo al primer acceso a biblioteca (no en launch). Mostrar diálogo de explicación (rationale) antes de solicitar. Detectar "Permanently Denied" y redirigir a Settings → Permisos. Implementar estados: NotRequested, Granted, Denied, PermanentlyDenied. Condicionar la pantalla Library a permiso concedido.
  Requirements: R6 | Design: Storage Manager, Error Handling

- [ ] 11. App Layer — Library Screen (Tracks, Albums, Artists)
  Crear LibraryViewModel con UiState (tracks, albums, artists, isLoading, error). Implementar pantalla Library con tabs: Tracks (LazyColumn), Albums (LazyVerticalGrid con thumbnails 64x64), Artists (LazyColumn). Implementar búsqueda con debounce 300ms. Implementar lazy loading de cover art con Coil + placeholders. Trigger escaneo inicial al primer acceso post-permiso. Mostrar progreso de escaneo. Implementar empty state. Track tap → play track y establecer contexto de cola.
  Requirements: R1, R2, R3 | Design: LibraryViewModel, Screens

- [ ] 12. App Layer — Now Playing Screen + Mini Player
  Crear PlayerViewModel con NowPlayingUiState (track, isPlaying, progress, shuffle, repeat, coverArt). Implementar pantalla NowPlaying: cover art grande, info (título/artista/álbum), seekbar con posición/duración, controles (prev/play-pause/next), toggles shuffle y repeat (Off→All→One). Implementar MiniPlayerBar persistente en otras pantallas. Conectar ViewModel a PlayerController StateFlows. Implementar animación de transición de cover art al cambiar canción.
  Requirements: R1, R2, R3 | Design: NowPlayingUiState, PlayerController

- [ ] 13. App Layer — Lyrics Screen (Sincronizadas + Plain)
  Crear LyricsViewModel que observa track actual y busca letras via GetSyncedLyricsUseCase. Implementar letras sincronizadas: auto-scroll a línea actual, highlight con color distinto, tolerancia de sync ≤500ms. Implementar letras plain text: scroll manual con font size ajustable. Mostrar fuente de letras (LRCLIB/Genius). Implementar estado "No disponible". Implementar loading state. Acceso via swipe-up desde NowPlaying o tab de navegación.
  Requirements: R4 | Design: Lyrics Screen, SyncedLine

- [ ] 14. App Layer — Playlists, History, EQ y Settings
  Crear PlaylistViewModel: listar playlists, crear nueva (dialog con nombre), agregar/remover tracks, reordenar con drag-and-drop. Implementar pantalla Playlists con lista y detalle. Implementar "Add to Playlist" desde context menu en tracks. Implementar pantalla History (últimas 100 canciones con timestamp, tap para reproducir). Crear SettingsViewModel: tema (Light/Dark/System), EQ (5 presets como chips + toggle enable), sleep timer (15/30/60/90/custom minutos con countdown y cancel), limpiar caché (mostrar tamaño, confirmar borrado). Implementar sliders de EQ que aplican cambios en <100ms via EqualizerController.
  Requirements: R2, R5 | Design: Playlists, Settings, EQ

- [ ] 15. Integration — Estado de reproducción persistente + Error Handling
  Implementar guardado automático de estado: al pausar, al hacer background, al cambiar track (debounce 5s). Restaurar estado al cold start: track, posición, cola, shuffle, repeat, EQ. Manejar gracefully si track restaurado ya no existe. Implementar error handling global: sealed Result en use cases, UiState con campo error en ViewModels, Snackbar para errores de usuario. Implementar skip automático en tracks con error de formato/corrupto. Implementar retry con backoff para APIs. Implementar handling de headphone disconnect (pause).
  Requirements: R1, R5, R6 | Design: Error Handling, SavedPlaybackState

- [ ] 16. Testing — Property Tests + Unit Tests + UI Tests
  Configurar Kotest Property Testing (100 iteraciones mínimo). Implementar 12 property tests del design: P1 Playback state metadata, P2 Pause preserves state, P3 Queue navigation, P4 Cover art chain ordering, P5 Cover art resolution, P6 Synced lyrics line selection, P7 Playlist round-trip, P8 Shuffle permutation, P9 History bounded buffer, P10 State serialization round-trip, P11 Scanner dir filtering, P12 Scanner corruption resilience. Unit tests: Use Cases con MockK, data mappers, EQ presets validation, format detection. Integration tests: Room DAOs con in-memory DB, Retrofit con MockWebServer (Last.fm, MusicBrainz, LRCLIB, Genius), Media3 playback cycle. UI tests: navegación entre 5 pantallas, NowPlaying controles, Library lazy loading.
  Requirements: All | Design: Correctness Properties, Testing Strategy

## Notes

- API keys requeridas: Last.fm (gratuita), Genius (gratuita). Almacenar en `local.properties`, nunca commitear.
- LRCLIB no requiere API key.
- MusicBrainz no requiere API key.
- Package name: `com.sonus.player`
- minSdk 29 (Android 10), targetSdk 34 (Android 14)
- Cada tarea debe compilar exitosamente antes de avanzar a la siguiente
- Property tests usan formato: `Feature: professional-music-player, Property {N}: {title}`
