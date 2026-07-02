# Requirements Document

## Introduction

Professional Music Player for Android es una aplicación de reproducción de música local con diseño minimalista y características avanzadas. El sistema permite a los usuarios reproducir archivos de música locales, mostrar información artística, visualizar letras de canciones y proporcionar una experiencia de usuario profesional con filosofía de diseño e ingeniería bien definida.

## Glossary

- **Music_Player**: La aplicación principal que maneja la reproducción de música, interfaz de usuario y funcionalidades avanzadas
- **Local_Music_Scanner**: Componente que escanea y organiza archivos de música locales del dispositivo
- **Metadata_Extractor**: Componente que extrae metadatos de archivos de música (artista, álbum, género, etc.)
- **Cover_Art_Resolver**: Componente que obtiene y muestra carátulas de álbumes basadas en información del artista
- **Lyrics_Provider**: Servicio que obtiene letras de canciones mediante APIs o IA
- **Audio_Engine**: Motor de reproducción de audio que maneja decodificación, ecualización y efectos
- **UI_Controller**: Controlador de interfaz de usuario que implementa el diseño minimalista
- **Configuration_Manager**: Gestor de configuración que maneja preferencias y opciones avanzadas
- **Storage_Manager**: Gestor de almacenamiento que maneja permisos y acceso a archivos locales
- **Playback_State**: Estado actual de reproducción (pausado, reproduciendo, detenido, etc.)
- **Music_Track**: Representación de una canción con sus metadatos y ubicación del archivo
- **Playlist**: Colección organizada de Music_Track para reproducción secuencial
- **Cover_Art**: Imagen de carátula del álbum asociada a un artista o canción
- **Lyrics**: Letra textual de una canción con información de sincronización temporal
- **EQ_Preset**: Configuración predefinida de ecualización para diferentes géneros musicales

## Requirements

### Requirement 1: Reproducción de Música Local

**User Story:** Como usuario de Android, quiero reproducir archivos de música locales almacenados en mi dispositivo, para poder escuchar mi colección personal sin necesidad de conexión a Internet.

#### Acceptance Criteria

1. THE Local_Music_Scanner SHALL descubrir todos los archivos de música compatibles en el almacenamiento local del dispositivo dentro de los siguientes directorios estándar de Android: /Music/, /Download/, /Documents/ y cualquier directorio específicamente especificado por el usuario
2. WHEN un usuario selecciona una canción, THE Music_Player SHALL comenzar la reproducción dentro de 500ms con una latencia de buffer máximo de 100ms
3. WHILE se está reproduciendo música, THE Music_Player SHALL mostrar información básica (título, artista, duración) actualizada cada 100ms para reflejar la posición de reproducción actual
4. WHEN el usuario presiona pausa, THE Music_Player SHALL detener temporalmente la reproducción manteniendo el estado actual de posición de reproducción con una precisión de ±50ms
5. WHEN el usuario presiona siguiente/anterior, THE Music_Player SHALL cambiar a la siguiente/anterior canción en la lista actual dentro de 300ms, omitiendo las canciones marcadas como no reproducibles
6. WHILE se reproduce música, THE Audio_Engine SHALL mantener una latencia de reproducción menor a 100ms y una tasa de error de muestreo menor a 0.01%
7. IF ocurre un error al cargar un archivo (error de formato, archivo corrupto, acceso denegado), THEN THE Music_Player SHALL mostrar un mensaje de error descriptivo incluyendo el nombre del archivo y el código de error específico, y continuar con la siguiente canción disponible en la lista
8. THE Local_Music_Scanner SHALL indexar nuevos archivos de música agregados al almacenamiento del dispositivo dentro de 2 minutos de su detección por el sistema operativo

### Requirement 2: Diseño Minimalista y Filosofía de Ingeniería

**User Story:** Como usuario que valora la estética y usabilidad, quiero una interfaz minimalista con filosofía de diseño e ingeniería bien definida, para tener una experiencia de usuario cohesiva y profesional.

#### Acceptance Criteria

1. THE UI_Controller SHALL implementar un diseño de interfaz minimalista definido como: máximo 3 colores principales, no más de 5 elementos visibles por pantalla principal, y jerarquía visual clara con contraste mínimo de 4.5:1, siguiendo los principios de Material Design 3 incluyendo componentes como FloatingActionButton, BottomAppBar y NavigationRail
2. WHILE la aplicación está en uso, THE UI_Controller SHALL mantener una tasa de refresco de al menos 60 fps en dispositivos con pantalla de 60Hz o superior, y mantener el jank (fotogramas que superan 16ms de renderización) por debajo del 5% durante sesiones de 10 minutos
3. WHEN se cambia entre vistas principales (Biblioteca, Reproducción, Búsqueda, Configuración), THE UI_Controller SHALL completar la transición en menos de 300ms con animaciones fluidas que no consuman más del 2% de CPU durante la transición
4. THE Configuration_Manager SHALL proporcionar opciones de personalización de tema (claro/oscuro/automático) que pueden cambiarse en menos de 100ms después de la selección del usuario y deben persistir entre reinicios de la aplicación
5. WHERE el modo oscuro está habilitado, THE UI_Controller SHALL aplicar una paleta de colores optimizada para visión nocturna definida como: color de fondo #121212, color de superficie #1E1E1E, color primario #BB86FC, color secundario #03DAC6, con contraste mínimo de 7:1 para texto
6. IF se detecta memoria baja en el dispositivo (menos del 100MB de RAM disponible), THEN THE UI_Controller SHALL reducir el uso de recursos gráficos manteniendo la funcionalidad básica mediante: deshabilitar animaciones complejas, reducir la resolución de imágenes de carátulas al 50%, y eliminar efectos de sombra y transparencia

### Requirement 3: Carátulas de Álbumes Avanzadas

**User Story:** Como entusiasta de la música, quiero ver carátulas de álbumes organizadas por artista/cantante, para tener una experiencia visual enriquecida mientras escucho música.

#### Acceptance Criteria

1. THE Metadata_Extractor SHALL extraer información de artista y álbum de los metadatos de los archivos de música, soportando formatos ID3v2 (MP3), Vorbis Comments (OGG/FLAC) y APE Tags (Monkey's Audio), con una precisión de extracción del 95% o superior
2. WHEN no hay carátula disponible en los metadatos locales, THE Cover_Art_Resolver SHALL buscar carátulas en línea basándose en el artista y nombre del álbum utilizando al menos 2 fuentes (Last.fm API y MusicBrainz API) con un timeout máximo de 3 segundos por solicitud
3. WHERE se encuentran múltiples carátulas para un álbum, THE Cover_Art_Resolver SHALL seleccionar la de mayor resolución disponible (mínimo 300x300 píxeles, preferiblemente 600x600 o superior) y cachearla localmente por 30 días
4. THE UI_Controller SHALL mostrar la carátula del álbum actual en la pantalla de reproducción principal con un tamaño mínimo de 200x200 píxeles en dispositivos móviles y proporcionalmente mayor en tabletas, con opción de vista ampliada al tocar
5. WHILE se navega por la biblioteca de música, THE UI_Controller SHALL mostrar miniaturas de carátulas para cada álbum con tamaño de 64x64 píxeles, cargadas mediante lazy loading cuando sean visibles en pantalla
6. IF no se puede obtener ninguna carátula (fuentes en línea no disponibles o sin resultados), THEN THE Cover_Art_Resolver SHALL generar una carátula por defecto basada en los colores dominantes del artista usando un gradiente que combine los colores hexadecimales derivados de un hash del nombre del artista
7. THE Cover_Art_Resolver SHALL permitir al usuario reemplazar manualmente cualquier carátula generada o descargada con una imagen local de su galería, que será almacenada de forma persistente y priorizada sobre versiones en línea

### Requirement 4: Letras de Canciones con APIs/IA

**User Story:** Como amante de la música, quiero ver las letras de las canciones que estoy escuchando, obtenidas mediante APIs o IA, para poder seguir la letra y entender mejor la canción.

#### Acceptance Criteria

1. THE Lyrics_Provider SHALL buscar letras de la canción actual utilizando múltiples fuentes de APIs disponibles (Genius API, Musixmatch API) y caches locales, con un timeout máximo de 2 segundos por fuente
2. WHEN se encuentran letras con sincronización temporal (timestamps en formato [mm:ss.ms]), THE Lyrics_Provider SHALL mostrar las letras sincronizadas con la reproducción manteniendo un desfase máximo de 500ms entre el timestamp y el momento de resaltado
3. WHERE no hay letras disponibles en APIs tradicionales, THE Lyrics_Provider SHALL utilizar modelos de IA (preferiblemente Whisper o similar) para generar aproximaciones de letras basadas en el audio con una latencia máxima de 200ms por segundo de audio procesado
4. THE UI_Controller SHALL mostrar las letras en un formato legible con tamaño de fuente ajustable entre 12px y 32px, espaciado de línea de 1.5, y contraste mínimo de 4.5:1 entre texto y fondo
5. IF se detecta que las letras generadas por IA tienen menos del 80% de confianza (basado en puntuación del modelo), THEN THE Lyrics_Provider SHALL mostrar una advertencia indicando "Letras aproximadas - confianza estimada: X%" donde X es el porcentaje redondeado al 5% más cercano
6. WHILE se muestran letras sincronizadas, THE UI_Controller SHALL resaltar la línea actualmente cantada en tiempo real con un color distintivo (por defecto amarillo #FFD700) y transición suave entre líneas (animación de 200ms)
7. IF ninguna fuente proporciona letras (APIs sin resultados y IA no disponible), THEN THE Lyrics_Provider SHALL mostrar un mensaje "Letras no disponibles" y ofrecer la opción de contribuir letras manualmente mediante un editor de texto integrado

### Requirement 5: Características Básicas Adicionales

**User Story:** Como usuario que busca funcionalidad completa, quiero características adicionales básicas de un reproductor profesional, para tener una experiencia de reproducción completa.

#### Acceptance Criteria

1. THE Configuration_Manager SHALL proporcionar configuración de ecualizador con al menos 5 presets predefinidos (rock: +6dB 100Hz, +3dB 1kHz; pop: +4dB 100Hz, +4dB 10kHz; clásica: plano; jazz: +3dB 100Hz, +2dB 3kHz; plano: todas bandas a 0dB) y 5 bandas de ecualización manual (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) con rango de ±12dB
2. WHEN se selecciona un preset de ecualizador, THE Audio_Engine SHALL aplicar los ajustes correspondientes dentro de 100ms y persistir la configuración seleccionada para sesiones futuras hasta que el usuario la cambie manualmente
3. THE Music_Player SHALL soportar la creación y gestión de listas de reproducción personalizadas con capacidad para al menos 1000 canciones por playlist, operaciones de arrastrar y soltar para reordenar, y exportación/importación en formato M3U
4. WHERE se habilita la función de aleatorio, THE Music_Player SHALL reproducir canciones en orden no determinístico usando un algoritmo Fisher-Yates shuffle garantizando que no se repita ninguna canción hasta que se hayan reproducido todas las canciones de la lista actual, o por un máximo de 100 canciones en listas mayores
5. THE Music_Player SHALL mantener historial de reproducción de las últimas 100 canciones reproducidas con información de fecha/hora de reproducción y permitir al usuario volver a cualquier canción del historial con un solo toque
6. IF la aplicación es cerrada durante la reproducción, THEN THE Configuration_Manager SHALL restaurar el estado de reproducción al reiniciar la aplicación incluyendo: canción actual, posición exacta (±100ms), lista de reproducción activa, modo de aleatorio, y configuración de ecualizador
7. THE Configuration_Manager SHALL permitir al usuario establecer temporizador de apagado automático con opciones de 15, 30, 60, 90 minutos o personalizado, que detendrá la reproducción suavemente con un fade-out de 3 segundos cuando expire

### Requirement 6: Gestión de Almacenamiento y Permisos

**User Story:** Como usuario preocupado por la privacidad y organización, quiero una gestión adecuada de permisos y almacenamiento, para que la aplicación funcione correctamente sin comprometer mi privacidad.

#### Acceptance Criteria

1. THE Storage_Manager SHALL solicitar permisos de almacenamiento (READ_EXTERNAL_STORAGE o MANAGE_EXTERNAL_STORAGE según versión de Android) solo cuando el usuario intente acceder por primera vez a su biblioteca de música, siguiendo las mejores prácticas de Android para solicitudes de permisos con explicación contextual
2. WHEN se deniegan permisos de almacenamiento, THE Storage_Manager SHALL proporcionar explicaciones claras indicando exactamente qué funcionalidad estará limitada (sin acceso a música local) y opciones para otorgarlos más tarde mediante un diálogo redirigiendo a Configuración → Aplicaciones → [App] → Permisos
3. THE Local_Music_Scanner SHALL escanear solo directorios de medios estándar (/Music/, /Download/, /Documents/, /Android/media/) y directorios específicamente permitidos por el usuario mediante un selector de carpetas nativo, ignorando directorios del sistema, aplicaciones y contenido sensible
4. WHERE se especifican carpetas excluidas por el usuario (máximo 10), THE Local_Music_Scanner SHALL omitirlas durante el escaneo y no mostrar su contenido en la biblioteca, incluso si contienen archivos de música compatibles
5. THE Configuration_Manager SHALL permitir al usuario limpiar caché de carátulas y letras descargadas con opciones selectivas (solo carátulas, solo letras, todo) mostrando el espacio liberado en MB/GB tras la operación
6. IF se detectan archivos corruptos durante el escaneo (cabeceras inválidas, checksums fallidos, tamaño cero), THEN THE Local_Music_Scanner SHALL registrarlos en un log interno omitiéndolos sin interrumpir el proceso general, y mostrar al usuario un resumen de archivos omitidos al finalizar el escaneo

### Requirement 7: Optimización de Rendimiento y Batería

**User Story:** Como usuario móvil, quiero que la aplicación sea eficiente en consumo de recursos y batería, para poder usarla durante largos períodos sin afectar significativamente la duración de la batería.

#### Acceptance Criteria

1. WHILE se reproduce música con la pantalla apagada, THE Music_Player SHALL consumir menos del 5% de batería por hora en dispositivos modernos (definidos como Android 10+, batería ≥4000mAh, SoC Snapdragon 730/Exynos 9611 o superior), medido con brillo al 50% y volumen al 70%
2. THE Audio_Engine SHALL utilizar técnicas de optimización de energía incluyendo wakelocks parciales (PARTIAL_WAKE_LOCK) solo durante reproducción activa, priorización de procesos (setPriority(THREAD_PRIORITY_AUDIO)), y uso eficiente de codecs de hardware cuando estén disponibles
3. WHEN la aplicación está en segundo plano por más de 30 minutos sin reproducción, THEN THE Configuration_Manager SHALL reducir el consumo de memoria liberando recursos no críticos (caché de imágenes >50MB, buffers de audio inactivos, objetos de UI no visibles) manteniendo al menos un 50% de reducción en uso de memoria
4. THE UI_Controller SHALL implementar carga diferida (lazy loading) para imágenes de carátulas en listas largas (>50 ítems), cargando solo las visibles en pantalla +5 ítems de margen, con placeholders de color sólido mientras se cargan
5. WHERE la conexión a Internet es limitada (velocidad <1 Mbps o datos móviles), THE Lyrics_Provider SHALL utilizar caché agresivo (retención de 24 horas para letras) y limitar las solicitudes de red a 1 por canción, mostrando primero letras cacheadas si disponibles
6. IF se detecta que la aplicación está consumiendo recursos excesivos (CPU >25% por más de 1 minuto, memoria >300MB en dispositivos con <4GB RAM), THEN THE Configuration_Manager SHALL ofrecer al usuario opciones de optimización incluyendo: reducir calidad de carátulas, deshabilitar animaciones, y limitar análisis de metadatos en segundo plano
7. THE Music_Player SHALL implementar un modo de ahorro de energía que pueda activarse manualmente o automáticamente cuando la batería esté por debajo del 20%, reduciendo consumo mediante: deshabilitar búsqueda automática de carátulas, limitar frecuencia de actualización de UI a 30fps, y usar codecs de menor consumo energético

### Requirement 8: Compatibilidad y Estándares

**User Story:** Como usuario con colección diversa de formatos musicales, quiero compatibilidad amplia con formatos y estándares, para poder reproducir toda mi colección sin problemas.

#### Acceptance Criteria

1. THE Audio_Engine SHALL soportar al menos los formatos MP3 (MPEG 1/2/2.5 Layer 3, 32-320kbps), AAC (LC, HE-AAC v1/v2, 8-512kbps), FLAC (nivel 0-8, 16-24 bit, 44.1-192kHz), OGG Vorbis (calidad -1 a 10, 8-500kbps) y WAV (PCM 8/16/24/32 bit, 8-384kHz)
2. THE Metadata_Extractor SHALL leer metadatos ID3v2 (v2.3, v2.4), Vorbis Comments y APE Tags (v1, v2) con soporte para campos estándar (título, artista, álbum, año, género, número de pista) y campos extendidos (compositor, letrista, discográfica, URL)
3. WHERE se encuentran archivos con metadatos en múltiples formatos (ej: ID3v2 + APE), THE Metadata_Extractor SHALL priorizar la información más completa (mayor número de campos válidos) y actualizada (timestamp más reciente si disponible), resolviendo conflictos mostrando ambas versiones al usuario para selección manual
4. THE Music_Player SHALL reproducir archivos con tasas de bits desde 64 kbps hasta 320 kbps (MP3/AAC) o 1000 kbps (FLAC) manteniendo una relación señal/ruido >90dB y distorsión armónica total <0.01% en dispositivos con soporte de hardware, verificable mediante análisis espectral
5. IF se encuentra un formato no compatible (ej: MIDI, MOD, DSD), THEN THE Music_Player SHALL mostrar un mensaje claro indicando "Formato no soportado: [nombre formato]. Formatos soportados: MP3, AAC, FLAC, OGG, WAV" y ofrecer opción para omitir o convertir mediante app externa si disponible
6. THE Audio_Engine SHALL manejar correctamente archivos con diferentes frecuencias de muestreo (44.1kHz, 48kHz, 88.2kHz, 96kHz, 176.4kHz, 192kHz) realizando resampling de alta calidad (mínimo 64-tap FIR filter) cuando el hardware no soporte la frecuencia nativa, manteniendo alias < -96dB
7. THE Music_Player SHALL respetar los flags de replay gain (RG) en metadatos cuando estén presentes, ajustando automáticamente el volumen para normalización entre pistas con un rango de ±12dB, manteniendo clipping por debajo del 0.1%