package io.github.reneknap.mediacenter.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

        private val _currentMediaItemIndex = MutableStateFlow(0)
        override val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

        private val _playWhenReady = MutableStateFlow(false)
        override val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()

        private var controller: MediaController? = null
        private val pendingCommands = mutableListOf<(MediaController) -> Unit>()

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
                    // End-of-playlist: reset playWhenReady so the next togglePlayPause is meaningful
                    // (without this, the user-intent flag stays "true" while playback is actually stopped).
                    if (playbackState == Player.STATE_ENDED) {
                        active.playWhenReady = false
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    val active = controller ?: return
                    _currentMediaItemIndex.value = active.currentMediaItemIndex
                    _positionMs.value = 0L
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    _playWhenReady.value = playWhenReady
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

        override fun setQueue(
            items: List<AudioTrack>,
            startIndex: Int,
            playWhenReady: Boolean,
            startPositionMs: Long?,
        ) {
            val mediaItems = items.map { it.toMediaItem() }
            withController { c ->
                // A null startPositionMs means "keep the current item playing where it is" — used when
                // re-pushing a reordered queue (shuffle toggle) so the current track does not restart.
                val resolvedPosition = startPositionMs ?: c.currentPosition
                c.setMediaItems(mediaItems, startIndex, resolvedPosition)
                c.prepare()
                c.playWhenReady = playWhenReady
                _positionMs.value = resolvedPosition
            }
        }

        override fun seekToNext() {
            withController { c -> c.seekToNextMediaItem() }
        }

        override fun seekToPrevious() {
            withController { c -> c.seekToPreviousMediaItem() }
        }

        override fun seekToMediaItem(index: Int) {
            withController { c -> c.seekToDefaultPosition(index) }
        }

        override fun moveMediaItem(
            fromIndex: Int,
            toIndex: Int,
        ) {
            withController { c ->
                c.moveMediaItem(fromIndex, toIndex)
                _currentMediaItemIndex.value = c.currentMediaItemIndex
            }
        }

        override fun removeMediaItem(index: Int) {
            withController { c ->
                c.removeMediaItem(index)
                _currentMediaItemIndex.value = c.currentMediaItemIndex
            }
        }

        override fun addMediaItem(
            index: Int,
            item: AudioTrack,
        ) {
            withController { c ->
                c.addMediaItem(index, item.toMediaItem())
                _currentMediaItemIndex.value = c.currentMediaItemIndex
            }
        }

        override fun seekTo(positionMs: Long) {
            withController { c ->
                c.seekTo(positionMs)
                // Reflect the new position immediately so the seek bar does not snap back
                // to the last polled value before the next poll tick.
                _positionMs.value = positionMs
            }
        }

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            withController { c ->
                c.playWhenReady = playWhenReady
            }
        }

        private fun connectController() {
            val token = SessionToken(context, ComponentName(context, MediaCenterPlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                val connected = future.get()
                connected.addListener(playerListener)
                controller = connected
                _currentMediaItemIndex.value = connected.currentMediaItemIndex
                _playWhenReady.value = connected.playWhenReady
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

private fun AudioTrack.toMediaItem(): MediaItem {
    val metadata =
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    return MediaItem.Builder()
        .setMediaId(uri)
        .setUri(uri.toUri())
        .setMediaMetadata(metadata)
        .build()
}
