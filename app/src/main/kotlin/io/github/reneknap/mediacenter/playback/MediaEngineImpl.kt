package io.github.reneknap.mediacenter.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        @ApplicationContext private val context: Context,
        @ApplicationScope appScope: CoroutineScope,
    ) : MediaEngine {
        private val scope = CoroutineScope(appScope.coroutineContext + Dispatchers.Main.immediate)

        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _positionMs = MutableStateFlow(0L)
        override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

        private val _durationMs = MutableStateFlow(0L)
        override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

        private var controller: MediaController? = null
        private val pendingCommands = mutableListOf<(MediaController) -> Unit>()

        private var trackEndedListener: (() -> Unit)? = null

        private val playerListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val active = controller ?: return
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = active.duration.coerceAtLeast(0L)
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        trackEndedListener?.invoke()
                    }
                }
            }

        init {
            connectController()
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
            withController { c ->
                c.setMediaItem(MediaItem.fromUri(track.uri.toUri()))
                c.prepare()
                c.playWhenReady = playWhenReady
                _positionMs.value = 0L
            }
        }

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            withController { c ->
                c.playWhenReady = playWhenReady
            }
        }

        override fun setOnTrackEndedListener(listener: () -> Unit) {
            trackEndedListener = listener
        }

        private fun connectController() {
            val token = SessionToken(context, ComponentName(context, MediaCenterPlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                val connected = future.get()
                connected.addListener(playerListener)
                controller = connected
                pendingCommands.forEach { it(connected) }
                pendingCommands.clear()
            }, ContextCompat.getMainExecutor(context))
        }

        private fun withController(block: (MediaController) -> Unit) {
            scope.launch {
                val c = controller
                if (c != null) {
                    block(c)
                } else {
                    pendingCommands.add(block)
                }
            }
        }

        private suspend fun pollPosition() {
            while (_isPlaying.value) {
                controller?.let { _positionMs.value = it.currentPosition }
                delay(POLL_INTERVAL_MS)
            }
        }

        private companion object {
            const val POLL_INTERVAL_MS = 500L
        }
    }
