package com.quranmedia.player.media.player

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enum representing the current state of athan playback
 */
enum class AthanPlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    COMPLETED,
    ERROR
}

/**
 * Player specifically for Athan (call to prayer) audio.
 * Normal mode uses notification stream (respects silent/DND).
 * High volume mode uses alarm stream (overrides silent, still respects DND).
 * Separate from QuranPlayer to avoid interfering with Quran recitation playback.
 */
@Singleton
class AthanPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _player: ExoPlayer? = null
    private var originalVolume: Int = -1
    private var volumeStream: Int = -1
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _playbackState = MutableStateFlow(AthanPlaybackState.IDLE)
    val playbackState: StateFlow<AthanPlaybackState> = _playbackState.asStateFlow()

    private val _currentAthanId = MutableStateFlow<String?>(null)
    val currentAthanId: StateFlow<String?> = _currentAthanId.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    private fun createPlayer(useAlarmStream: Boolean): ExoPlayer {
        // Create HTTP data source factory for streaming
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AlFurqan/1.0")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        // Create data source factory that supports both HTTP streaming and local files
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Create media source factory with support for both HTTP and file URIs
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Normal mode: USAGE_NOTIFICATION respects silent mode and DND
        // High volume mode: USAGE_ALARM overrides silent but DND still respected
        val usage = if (useAlarmStream) C.USAGE_ALARM else C.USAGE_NOTIFICATION

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(usage)
                    .build(),
                false  // Don't handle audio focus automatically - we manage it
            )
            .setHandleAudioBecomingNoisy(false)  // Don't pause when headphones disconnected
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(playerListener)
                Timber.d("AthanPlayer created with ${if (useAlarmStream) "alarm" else "notification"} audio attributes")
            }
            .also { _player = it }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Timber.d("Athan buffering")
                    _playbackState.value = AthanPlaybackState.LOADING
                }
                Player.STATE_READY -> {
                    Timber.d("Athan ready")
                    if (_player?.isPlaying == true) {
                        _playbackState.value = AthanPlaybackState.PLAYING
                    }
                }
                Player.STATE_ENDED -> {
                    Timber.d("Athan playback ended")
                    _playbackState.value = AthanPlaybackState.COMPLETED
                    restoreVolume()
                    onCompletionCallback?.invoke()
                    onCompletionCallback = null
                }
                Player.STATE_IDLE -> {
                    Timber.d("Athan idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("Athan isPlaying: $isPlaying")
            if (isPlaying) {
                _playbackState.value = AthanPlaybackState.PLAYING
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Athan playback error")
            _playbackState.value = AthanPlaybackState.ERROR
            restoreVolume()
            onCompletionCallback?.invoke()
            onCompletionCallback = null
        }
    }

    /**
     * Play athan from a path (local file or URL)
     * This is the primary method - use AthanRepository to get the correct path
     * @param path Local file path or streaming URL
     * @param athanId Optional athan ID for tracking
     * @param maximizeVolume Whether to set volume to maximum (default true)
     * @param onCompletion Callback when athan playback completes
     */
    fun playAthan(
        path: String,
        athanId: String? = null,
        maximizeVolume: Boolean = true,
        onCompletion: (() -> Unit)? = null
    ) {
        try {
            Timber.d("Playing athan from path: $path")

            // Stop any current playback
            stop()

            // Always recreate player with correct audio attributes (immutable after creation)
            _player?.release()
            _player = null
            val player = createPlayer(useAlarmStream = maximizeVolume)

            // Store callback
            onCompletionCallback = onCompletion

            // Maximize volume if requested
            if (maximizeVolume) {
                saveAndMaximizeVolume()
            }

            // Prepare and play - MediaItem handles both file:// and http:// URIs
            val mediaItem = MediaItem.fromUri(path)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            _currentAthanId.value = athanId
            _playbackState.value = AthanPlaybackState.LOADING

            Timber.d("Athan playback started")
        } catch (e: Exception) {
            Timber.e(e, "Error playing athan")
            _playbackState.value = AthanPlaybackState.ERROR
            restoreVolume()
            onCompletion?.invoke()
        }
    }

    /**
     * Stop athan playback
     */
    fun stop() {
        try {
            _player?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.clearMediaItems()
            }
            _playbackState.value = AthanPlaybackState.IDLE
            _currentAthanId.value = null
            restoreVolume()
            onCompletionCallback = null
            Timber.d("Athan playback stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping athan")
        }
    }

    /**
     * Check if athan is currently playing
     */
    fun isPlaying(): Boolean {
        return _player?.isPlaying == true
    }

    /**
     * Release player resources
     */
    fun release() {
        try {
            stop()
            _player?.release()
            _player = null
            Timber.d("AthanPlayer released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing athan player")
        }
    }

    /**
     * Save current volume and set to maximum for athan.
     * Uses STREAM_ALARM when maximizeVolume is true, STREAM_NOTIFICATION otherwise.
     */
    private fun saveAndMaximizeVolume() {
        try {
            volumeStream = AudioManager.STREAM_ALARM
            originalVolume = audioManager.getStreamVolume(volumeStream)

            val maxVolume = audioManager.getStreamMaxVolume(volumeStream)
            audioManager.setStreamVolume(
                volumeStream,
                maxVolume,
                0  // No flags - silent change
            )
            Timber.d("Volume maximized for athan on stream $volumeStream (was $originalVolume, now $maxVolume)")
        } catch (e: Exception) {
            Timber.e(e, "Error maximizing volume")
        }
    }

    /**
     * Restore volume to original level on the stream that was maximized
     */
    private fun restoreVolume() {
        try {
            if (originalVolume >= 0 && volumeStream >= 0) {
                audioManager.setStreamVolume(
                    volumeStream,
                    originalVolume,
                    0  // No flags - silent change
                )
                Timber.d("Volume restored to $originalVolume on stream $volumeStream")
                originalVolume = -1
                volumeStream = -1
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restoring volume")
        }
    }
}
