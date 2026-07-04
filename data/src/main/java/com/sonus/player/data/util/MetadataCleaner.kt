package com.sonus.player.data.util

/**
 * Utility for cleaning music metadata before sending to external APIs (LRCLIB, Genius, Last.fm).
 *
 * This does NOT modify the database or what the user sees.
 * It only produces clean strings for search queries.
 */
object MetadataCleaner {

    /**
     * Full cleaning pipeline. Returns clean (artist, title, album).
     */
    fun clean(artist: String, title: String, album: String): CleanMetadata {
        var cleanArtist = cleanArtist(artist)
        var cleanTitle = cleanTitle(title)
        var cleanAlbum = cleanAlbum(album)

        // If title contains "Artist - Song", extract both
        val extracted = extractArtistFromTitle(cleanTitle)
        if (extracted != null) {
            // Only use extracted artist if current artist looks like garbage
            if (isGarbageArtist(cleanArtist)) {
                cleanArtist = extracted.first
            }
            cleanTitle = extracted.second
        }

        // If artist is still garbage, try to use the one from title extraction
        // even if current artist doesn't look like complete garbage
        if (extracted != null && cleanArtist != extracted.first) {
            // If extracted artist is a substring of the title's artist portion, prefer extracted
            val titleArtist = extracted.first
            if (titleArtist.length > cleanArtist.length / 2) {
                // Keep the cleaner one — heuristic: shorter is usually cleaner
                // unless the short one is too short (< 3 chars)
                if (cleanArtist.length > titleArtist.length && titleArtist.length >= 3) {
                    cleanArtist = titleArtist
                }
            }
        }

        // If album is essentially the same as "Artist - Title (junk)", simplify it
        if (cleanAlbum.contains(cleanTitle) || cleanAlbum == cleanTitle) {
            cleanAlbum = cleanTitle
        }

        return CleanMetadata(
            artist = cleanArtist,
            title = cleanTitle,
            album = cleanAlbum
        )
    }

    data class CleanMetadata(
        val artist: String,
        val title: String,
        val album: String
    )

    // ============================================================
    // ARTIST CLEANING
    // ============================================================

    fun cleanArtist(artist: String): String {
        var result = artist.trim()

        // Remove known suffixes (YouTube channels, labels, etc.)
        result = result
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*Official$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*Oficial$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Official\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*Official\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*Music\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*Records\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*TV\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*HD\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()

        // CamelCase separation (OliviaRodrigo → Olivia Rodrigo)
        if (!result.contains(" ") && result.length > 3) {
            val separated = result.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            if (separated.contains(" ")) {
                result = separated
            }
        }

        // Remove leading/trailing special characters
        result = result.replace(Regex("^[\\-_\\s]+|[\\-_\\s]+$"), "")

        return result.ifEmpty { artist.trim() }
    }

    // ============================================================
    // TITLE CLEANING
    // ============================================================

    fun cleanTitle(title: String): String {
        var result = title.trim()

        // Remove video/audio quality suffixes
        result = result
            .replace(Regex("\\s*\\(Official Video\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Official Music Video\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Video Oficial\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Lyric Video\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Lyrics\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Audio\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Official Audio\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Audio Oficial\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Letra/Lyrics\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Letra\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Visualizer\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Live\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(En Vivo\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Official Video]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Official Audio]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[HQ]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[HD]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[4K]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Explicit]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Explicit\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Clean]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Remastered\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Remastered \\d{4}\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Deluxe\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Deluxe Edition\\)$", RegexOption.IGNORE_CASE), "")
            .trim()

        // Remove track number prefix (01 - Song, 01. Song, 1 - Song)
        result = result
            .replace(Regex("^\\d{1,3}\\s*[\\-.]\\s*"), "")
            .replace(Regex("^\\d{1,3}\\s+"), "")
            .trim()

        // Remove leading/trailing special characters
        result = result.replace(Regex("^[\\-_\\s]+|[\\-_\\s]+$"), "")

        return result.ifEmpty { title.trim() }
    }

    // ============================================================
    // ALBUM CLEANING
    // ============================================================

    fun cleanAlbum(album: String): String {
        var result = album.trim()

        // Apply same suffix removal as title
        result = result
            .replace(Regex("\\s*\\(Official Video\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Official Music Video\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Video Oficial\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Deluxe\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Deluxe Edition\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Remastered\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Remastered \\d{4}\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Explicit]$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Explicit\\)$", RegexOption.IGNORE_CASE), "")
            .trim()

        // If album contains "Artist - Title", extract just the title part
        if (result.contains(" - ")) {
            val parts = result.split(" - ", limit = 2)
            if (parts.size == 2 && parts[1].isNotBlank()) {
                result = parts[1].trim()
                // Also clean suffix from extracted part
                result = result
                    .replace(Regex("\\s*\\(Official Video\\)$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*\\(Video Oficial\\)$", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
        }

        // Remove leading/trailing special characters
        result = result.replace(Regex("^[\\-_\\s]+|[\\-_\\s]+$"), "")

        return result.ifEmpty { album.trim() }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Extracts artist and title from a string like "Artist - Song Title"
     * Returns Pair(artist, title) or null if pattern not found.
     */
    private fun extractArtistFromTitle(title: String): Pair<String, String>? {
        if (!title.contains(" - ")) return null

        val parts = title.split(" - ", limit = 2)
        if (parts.size != 2) return null

        val extractedArtist = parts[0].trim()
        val extractedTitle = parts[1].trim()

        // Validate: both parts should be meaningful (> 1 char)
        if (extractedArtist.length < 2 || extractedTitle.length < 2) return null

        return Pair(extractedArtist, extractedTitle)
    }

    /**
     * Detects if an artist name is likely garbage/auto-generated.
     */
    private fun isGarbageArtist(artist: String): Boolean {
        val garbagePatterns = listOf(
            "unknown artist",
            "various artists",
            "unknown",
            "va",
            "n/a",
            "none",
            "track",
            "audio",
            "recording"
        )

        val lower = artist.lowercase().trim()

        // Check known garbage strings
        if (garbagePatterns.any { lower == it || lower.startsWith(it) }) return true

        // Looks like a filename (has extensions or weird chars)
        if (lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".flac")) return true

        // Too short to be a real artist name
        if (lower.length < 2) return true

        // All numbers
        if (lower.all { it.isDigit() || it == ' ' }) return true

        return false
    }
}
