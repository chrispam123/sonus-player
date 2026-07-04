package com.sonus.player.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sonus.player.domain.controller.PlayerController
import com.sonus.player.domain.model.PlaybackProgress
import com.sonus.player.domain.model.PlaybackState
import com.sonus.player.domain.model.RepeatMode
import com.sonus.player.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerController: EqualizerControllerImpl
) : PlayerController {

    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(
        PlaybackState(
            isPlaying = false,
            currentTrack = null,
            positionMs = 0L,
            durationMs = 0L,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            sleepTimerRemainingMs = null
        )
    )
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress(0L, 0L, 0L))
    override val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    override val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private var currentQueue: List<Track> = emptyList()
    private var currentIndex: Int = 0

    suspend fun connect() {
        if (mediaController != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, "com.sonus.player.playback.PlaybackService")
        )
        mediaController = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .await()

        mediaController?.addListener(playerListener)
        startProgressUpdates()
    }

    /**
     * Call this from the app layer to attach the equalizer once the service is running.
     */
    fun attachEqualizer(audioSessionId: Int) {
        if (audioSessionId != 0) {
            equalizerController.attachToSession(audioSessionId)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = mediaController?.currentMediaItemIndex ?: 0
            if (index in currentQueue.indices) {
                currentIndex = index
                _currentTrack.value = currentQueue[index]
            }
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updatePlaybackState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updatePlaybackState()
        }
    }

    private fun updatePlaybackState() {
        val controller = mediaController ?: return
        _playbackState.value = PlaybackState(
            isPlaying = controller.isPlaying,
            currentTrack = _currentTrack.value,
            positionMs = controller.currentPosition,
            durationMs = controller.duration.coerceAtLeast(0),
            shuffleEnabled = controller.shuffleModeEnabled,
            repeatMode = mapRepeatMode(controller.repeatMode),
            sleepTimerRemainingMs = null
        )
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    _progress.value = PlaybackProgress(
                        positionMs = controller.currentPosition,
                        durationMs = controller.duration.coerceAtLeast(0),
                        bufferedPositionMs = controller.bufferedPosition
                    )
                }
                delay(100) // update every 100ms
            }
        }
    }

    override fun play(track: Track) {
        playQueue(listOf(track), startIndex = 0)
    }

    override fun playQueue(tracks: List<Track>, startIndex: Int) {
        val controller = mediaController ?: return
        currentQueue = tracks
        currentIndex = startIndex
        _queue.value = tracks
        _currentTrack.value = tracks[startIndex]

        val mediaItems = tracks.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun resume() {
        mediaController?.play()
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun next() {
        mediaController?.seekToNextMediaItem()
    }

    override fun previous() {
        val controller = mediaController ?: return
        // If more than 3 seconds into the song, restart it. Otherwise go to previous.
        if (controller.currentPosition > 3000) {
            controller.seekTo(0)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: RepeatMode) {
        mediaController?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setSleepTimer(durationMs: Long) {
        // Will be implemented in Bala 4B (Settings/Sleep Timer)
    }

    override fun cancelSleepTimer() {
        // Will be implemented in Bala 4B
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()

        return MediaItem.Builder()
            .setUri(Uri.parse(filePath))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun mapRepeatMode(exoRepeatMode: Int): RepeatMode = when (exoRepeatMode) {
        Player.REPEAT_MODE_OFF -> RepeatMode.OFF
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }
}
