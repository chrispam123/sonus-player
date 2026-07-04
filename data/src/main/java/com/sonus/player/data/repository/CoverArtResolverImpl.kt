package com.sonus.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.sonus.player.data.local.dao.CoverArtDao
import com.sonus.player.data.local.entity.CoverArtCacheEntity
import com.sonus.player.data.remote.lastfm.LastFmApiService
import com.sonus.player.data.remote.musicbrainz.MusicBrainzApiService
import com.sonus.player.data.util.MetadataCleaner
import com.sonus.player.domain.model.CoverArtResult
import com.sonus.player.domain.model.Track
import com.sonus.player.domain.repository.CoverArtRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.abs

class CoverArtResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coverArtDao: CoverArtDao,
    private val lastFmApi: LastFmApiService,
    private val musicBrainzApi: MusicBrainzApiService
) : CoverArtRepository {

    companion object {
        private val LASTFM_API_KEY = com.sonus.player.data.BuildConfig.LASTFM_API_KEY
        private const val CACHE_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    override suspend fun getCoverArt(track: Track): CoverArtResult {
        // Step 1: Try embedded art via Android's album art URI
        val embeddedUri = getEmbeddedArtUri(track)
        if (embeddedUri != null) {
            return CoverArtResult.Remote(embeddedUri.toString())
        }

        // Step 2: Check local cache
        val cached = coverArtDao.getByAlbumId(track.albumId)
        if (cached != null && !isCacheExpired(cached)) {
            return when (cached.sourceType) {
                "generated" -> {
                    val colors = cached.colors?.split(",")?.map { it.trim().toInt() } ?: generateColors(track.artist)
                    CoverArtResult.Generated(colors)
                }
                else -> {
                    cached.url?.let { CoverArtResult.Remote(it) } ?: CoverArtResult.NotFound
                }
            }
        }

        // Step 3: Try Last.fm API
        val lastFmUrl = tryLastFm(track)
        if (lastFmUrl != null) {
            cacheResult(track.albumId, "lastfm", lastFmUrl)
            return CoverArtResult.Remote(lastFmUrl)
        }

        // Step 4: Try MusicBrainz
        val musicBrainzUrl = tryMusicBrainz(track)
        if (musicBrainzUrl != null) {
            cacheResult(track.albumId, "musicbrainz", musicBrainzUrl)
            return CoverArtResult.Remote(musicBrainzUrl)
        }

        // Step 5: Generate gradient
        val colors = generateColors(track.artist)
        cacheGenerated(track.albumId, colors)
        return CoverArtResult.Generated(colors)
    }

    override suspend fun clearCache() {
        coverArtDao.clearAll()
    }

    private fun getEmbeddedArtUri(track: Track): Uri? {
        return try {
            val albumArtUri = Uri.parse("content://media/external/audio/albumart")
            ContentUris.withAppendedId(albumArtUri, track.albumId)
        } catch (e: Exception) {
            null
        }
    }

    private fun isCacheExpired(cached: CoverArtCacheEntity): Boolean {
        return System.currentTimeMillis() - cached.fetchedAt > CACHE_VALIDITY_MS
    }

    private suspend fun tryLastFm(track: Track): String? {
        if (LASTFM_API_KEY.isEmpty()) return null
        return try {
            val clean = MetadataCleaner.clean(track.artist, track.title, track.album)
            val response = lastFmApi.getAlbumInfo(
                artist = clean.artist,
                album = clean.album,
                apiKey = LASTFM_API_KEY
            )
            response.album?.image
                ?.filter { it.url.isNotBlank() }
                ?.lastOrNull()
                ?.url
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryMusicBrainz(track: Track): String? {
        return try {
            val clean = MetadataCleaner.clean(track.artist, track.title, track.album)
            val query = "artist:${clean.artist} AND release:${clean.album}"
            val response = musicBrainzApi.searchRelease(query)
            val releaseId = response.releases?.firstOrNull()?.id ?: return null
            "https://coverartarchive.org/release/$releaseId/front-500"
        } catch (e: Exception) {
            null
        }
    }

    private fun generateColors(artist: String): List<Int> {
        // Deterministic gradient based on artist name hash
        val hash = artist.hashCode()
        val hue1 = abs(hash % 360).toFloat()
        val hue2 = (hue1 + 60f) % 360f // Complementary-ish color

        val color1 = android.graphics.Color.HSVToColor(floatArrayOf(hue1, 0.6f, 0.8f))
        val color2 = android.graphics.Color.HSVToColor(floatArrayOf(hue2, 0.5f, 0.6f))
        return listOf(color1, color2)
    }

    private suspend fun cacheResult(albumId: Long, source: String, url: String) {
        coverArtDao.insert(
            CoverArtCacheEntity(
                albumId = albumId,
                sourceType = source,
                url = url,
                colors = null,
                fetchedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun cacheGenerated(albumId: Long, colors: List<Int>) {
        coverArtDao.insert(
            CoverArtCacheEntity(
                albumId = albumId,
                sourceType = "generated",
                url = null,
                colors = colors.joinToString(","),
                fetchedAt = System.currentTimeMillis()
            )
        )
    }
}
