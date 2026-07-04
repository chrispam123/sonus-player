package com.sonus.player.data.repository

import android.util.Log
import com.sonus.player.data.local.dao.LyricsDao
import com.sonus.player.data.local.entity.LyricsCacheEntity
import com.sonus.player.data.remote.genius.GeniusApiService
import com.sonus.player.data.remote.lrclib.LrcLibApiService
import com.sonus.player.data.util.MetadataCleaner
import com.sonus.player.domain.model.LyricsResult
import com.sonus.player.domain.model.SyncedLine
import com.sonus.player.domain.model.SyncedLyricsResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    private val lyricsDao: LyricsDao,
    private val lrcLibApi: LrcLibApiService,
    private val geniusApi: GeniusApiService
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepo"
        private val GENIUS_TOKEN = com.sonus.player.data.BuildConfig.GENIUS_ACCESS_TOKEN
    }

    override suspend fun getLyrics(track: Track): LyricsResult {
        // Step 1: Check cache
        val cached = lyricsDao.getByTrackId(track.id)
        if (cached != null) {
            val text = cached.plainText ?: cached.syncedLyrics
            if (text != null) {
                Log.d(TAG, "Cache hit for: ${track.artist} - ${track.title}")
                return LyricsResult.Found(text)
            }
        }

        // Step 2: Try LRCLIB
        Log.d(TAG, "Trying LRCLIB for: ${track.artist} - ${track.title}")
        val lrcLibResult = tryLrcLib(track)
        if (lrcLibResult != null) {
            val text = lrcLibResult.first ?: lrcLibResult.second
            if (text != null) {
                cacheLyrics(track.id, lrcLibResult.first, lrcLibResult.second, "lrclib")
                Log.d(TAG, "LRCLIB found lyrics for: ${track.title}")
                return LyricsResult.Found(text)
            }
        }

        // Step 3: Try Genius (full scraping)
        Log.d(TAG, "Trying Genius for: ${track.artist} - ${track.title}")
        val geniusText = tryGeniusWithScraping(track)
        if (geniusText != null) {
            cacheLyrics(track.id, null, geniusText, "genius")
            Log.d(TAG, "Genius found lyrics for: ${track.title}")
            return LyricsResult.Found(geniusText)
        }

        Log.d(TAG, "No lyrics found for: ${track.artist} - ${track.title}")
        return LyricsResult.NotFound
    }

    override suspend fun getSyncedLyrics(track: Track): SyncedLyricsResult {
        // Step 1: Check cache for synced
        val cached = lyricsDao.getByTrackId(track.id)
        if (cached?.syncedLyrics != null) {
            val lines = parseLrc(cached.syncedLyrics)
            if (lines.isNotEmpty()) return SyncedLyricsResult.Found(lines)
        }

        // Step 2: Try LRCLIB for synced
        val lrcLibResult = tryLrcLib(track)
        if (lrcLibResult?.first != null) {
            cacheLyrics(track.id, lrcLibResult.first, lrcLibResult.second, "lrclib")
            val lines = parseLrc(lrcLibResult.first!!)
            if (lines.isNotEmpty()) return SyncedLyricsResult.Found(lines)
        }

        // Cache plain if available
        if (lrcLibResult?.second != null) {
            cacheLyrics(track.id, null, lrcLibResult.second, "lrclib")
        }

        return SyncedLyricsResult.NotFound
    }

    /**
     * Returns Pair(syncedLyrics, plainLyrics) or null if LRCLIB has nothing.
     */
    private suspend fun tryLrcLib(track: Track): Pair<String?, String?>? {
        return try {
            val clean = MetadataCleaner.clean(track.artist, track.title, track.album)
            val durationSec = (track.duration / 1000).toInt()
            Log.d(TAG, "LRCLIB request: artist='${clean.artist}', track='${clean.title}', album='${clean.album}', duration=$durationSec")

            val response = lrcLibApi.getLyrics(
                artist = clean.artist,
                trackName = clean.title,
                albumName = clean.album,
                duration = durationSec
            )

            Log.d(TAG, "LRCLIB response: synced=${response.syncedLyrics?.take(50)}, plain=${response.plainLyrics?.take(50)}")

            if (response.syncedLyrics != null || response.plainLyrics != null) {
                Pair(response.syncedLyrics, response.plainLyrics)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "LRCLIB error: ${e.message}")
            null
        }
    }

    /**
     * Searches Genius for the song, then scrapes the lyrics page with Jsoup.
     */
    private suspend fun tryGeniusWithScraping(track: Track): String? = withContext(Dispatchers.IO) {
        if (GENIUS_TOKEN.isEmpty()) {
            Log.d(TAG, "Genius: no token configured")
            return@withContext null
        }

        try {
            val clean = MetadataCleaner.clean(track.artist, track.title, track.album)
            val query = "${clean.artist} ${clean.title}"
            Log.d(TAG, "Genius search: '$query'")

            val response = geniusApi.search(
                query = query,
                token = "Bearer $GENIUS_TOKEN"
            )

            val songUrl = response.response?.hits?.firstOrNull()?.result?.url
            if (songUrl == null) {
                Log.d(TAG, "Genius: no results found")
                return@withContext null
            }

            Log.d(TAG, "Genius: found URL $songUrl")

            val lyrics = scrapeLyricsFromGenius(songUrl)
            lyrics
        } catch (e: Exception) {
            Log.e(TAG, "Genius error: ${e.message}")
            null
        }
    }

    /**
     * Cleans artist name by removing common suffixes from YouTube/streaming rips.
     */
    private fun cleanArtistName(artist: String): String = MetadataCleaner.cleanArtist(artist)

    /**
     * Cleans track title by removing common suffixes and extracting actual song name.
     */
    private fun cleanTrackTitle(title: String): String = MetadataCleaner.cleanTitle(title)

    /**
     * Scrapes lyrics text from a Genius song page using Jsoup.
     * Genius uses <div data-lyrics-container="true"> for lyrics content.
     */
    private fun scrapeLyricsFromGenius(url: String): String? {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .timeout(5000)
                .get()

            // Genius uses divs with data-lyrics-container="true"
            val lyricsContainers = doc.select("div[data-lyrics-container=true]")

            if (lyricsContainers.isEmpty()) {
                Log.d(TAG, "Genius scrape: no lyrics containers found")
                return null
            }

            val lyrics = buildString {
                for (container in lyricsContainers) {
                    // Replace <br> with newlines before getting text
                    container.select("br").forEach { it.before("\\n") }
                    val text = container.text().replace("\\n", "\n")
                    append(text)
                    append("\n")
                }
            }.trim()

            if (lyrics.isNotEmpty()) lyrics else null
        } catch (e: Exception) {
            Log.e(TAG, "Genius scrape error: ${e.message}")
            null
        }
    }

    /**
     * Parses LRC format string into List<SyncedLine>
     * Format: [mm:ss.xx]Line text
     */
    private fun parseLrc(lrcContent: String): List<SyncedLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        val lines = mutableListOf<SyncedLine>()

        for (line in lrcContent.lines()) {
            val match = regex.find(line) ?: continue
            val (minutes, seconds, centiseconds, text) = match.destructured
            if (text.isBlank()) continue

            val ms = minutes.toLong() * 60_000 +
                     seconds.toLong() * 1_000 +
                     centiseconds.padEnd(3, '0').take(3).toLong()

            lines.add(SyncedLine(text = text.trim(), startTimeMs = ms, endTimeMs = null))
        }

        // Set endTimeMs for each line (start of next line)
        for (i in 0 until lines.size - 1) {
            lines[i] = lines[i].copy(endTimeMs = lines[i + 1].startTimeMs)
        }

        return lines
    }

    private suspend fun cacheLyrics(trackId: Long, synced: String?, plain: String?, source: String) {
        lyricsDao.insert(
            LyricsCacheEntity(
                trackId = trackId,
                syncedLyrics = synced,
                plainText = plain,
                source = source,
                fetchedAt = System.currentTimeMillis()
            )
        )
    }
}
