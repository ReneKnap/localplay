package io.github.reneknap.mediacenter.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaEngineImpl
    @Inject
    constructor(
        private val player: ExoPlayer,
        @ApplicationScope appScope: CoroutineScope,
    ) : MediaEngine {
        private val scope = CoroutineScope(appScope.coroutineContext + Dispatchers.Main.immediate)

        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _positionMs = MutableStateFlow(0L)
        override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

        private val _durationMs = MutableStateFlow(0L)
        override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

        private var trackEndedListener: (() -> Unit)? = null

        private val playerListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        trackEndedListener?.invoke()
                    }
                }
            }

        init {
            scope.launch {
                player.addListener(playerListener)
            }
            scope.launch {
                _isPlaying.collectLatest { playing ->
                    if (playing) {
                        pollPosition()
                    }
                }
            }
        }

        override fun loadTrack(
            track: AudioTrack,
            playWhenReady: Boolean,
        ) {
            scope.launch {
                player.setMediaItem(MediaItem.fromUri(track.uri.toUri()))
                player.prepare()
                player.playWhenReady = playWhenReady
                _positionMs.value = 0L
            }
        }

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            scope.launch {
                player.playWhenReady = playWhenReady
            }
        }

        override fun setOnTrackEndedListener(listener: () -> Unit) {
            trackEndedListener = listener
        }

        private suspend fun pollPosition() {
            while (_isPlaying.value) {
                _positionMs.value = player.currentPosition
                kotlinx.coroutines.delay(POLL_INTERVAL_MS)
            }
        }

        private companion object {
            const val POLL_INTERVAL_MS = 500L
        }
    }
