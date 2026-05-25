package io.github.reneknap.mediacenter.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reneknap.mediacenter.data.audio.ArtworkReader
import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.data.video.ExternalSubtitle
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
        private val artworkReader: ArtworkReader,
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

        private val _player = MutableStateFlow<Player?>(null)
        override val player: StateFlow<Player?> = _player.asStateFlow()

        private val _textTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
        override val textTracks: StateFlow<List<SubtitleTrack>> = _textTracks.asStateFlow()

        private val _activeTextTrackId = MutableStateFlow<String?>(null)
        override val activeTextTrackId: StateFlow<String?> = _activeTextTrackId.asStateFlow()

        private var controller: MediaController? = null
        private val pendingCommands = mutableListOf<(MediaController) -> Unit>()

        // Maps a UI track id to the override that selects it; rebuilt on every onTracksChanged.
        private var textOverrides: Map<String, TrackSelectionOverride> = emptyMap()

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
                    refreshPosterForCurrentItem()
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    _playWhenReady.value = playWhenReady
                }

                override fun onTracksChanged(tracks: Tracks) {
                    mapTextTracks(tracks)
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
            items: List<MediaEntry>,
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
            item: MediaEntry,
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

        override fun selectTextTrack(id: String) {
            val override = textOverrides[id] ?: return
            withController { c ->
                c.trackSelectionParameters =
                    c.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(override)
                        .build()
            }
        }

        override fun disableSubtitles() {
            withController { c ->
                c.trackSelectionParameters =
                    c.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
            }
        }

        private fun mapTextTracks(tracks: Tracks) {
            val overrides = mutableMapOf<String, TrackSelectionOverride>()
            val list = mutableListOf<SubtitleTrack>()
            var active: String? = null
            tracks.groups
                .filter { it.type == C.TRACK_TYPE_TEXT }
                .forEachIndexed { groupIndex, group ->
                    for (trackIndex in 0..<group.length) {
                        if (!group.isTrackSupported(trackIndex)) continue
                        val format = group.getTrackFormat(trackIndex)
                        val id = "g${groupIndex}t$trackIndex"
                        overrides[id] = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                        list += SubtitleTrack(id = id, label = labelFor(format), language = format.language)
                        if (group.isTrackSelected(trackIndex)) active = id
                    }
                }
            textOverrides = overrides
            _textTracks.value = list
            _activeTextTrackId.value = active
        }

        // Video files carry no embedded cover, so the system media controls would show only a generic
        // placeholder. Extract a representative frame off-thread and hand it to them as artworkData
        // bytes (never artworkUri: the SAF content uri is not readable by SystemUI). Gated on the video
        // mediaType so audio never reaches the extractor and keeps its own embedded cover untouched.
        private fun refreshPosterForCurrentItem() {
            val active = controller ?: return
            val item = active.currentMediaItem ?: return
            if (item.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_VIDEO) return
            if (item.mediaMetadata.artworkData != null) return
            val uri = item.mediaId
            val index = active.currentMediaItemIndex
            scope.launch {
                val bytes = artworkReader.loadVideoFrameBytes(uri) ?: return@launch
                applyPoster(index, uri, bytes)
            }
        }

        private fun applyPoster(
            index: Int,
            uri: String,
            bytes: ByteArray,
        ) {
            val active = controller ?: return
            if (index !in 0..<active.mediaItemCount) return
            val item = active.getMediaItemAt(index)
            // The user may have skipped or reordered while the frame decoded; only update when the same
            // item is still there and still has no poster (prevents overwriting and a replace loop).
            if (item.mediaId != uri || item.mediaMetadata.artworkData != null) return
            val metadata =
                item.mediaMetadata
                    .buildUpon()
                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
            active.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
        }

        private fun connectController() {
            val token = SessionToken(context, ComponentName(context, MediaCenterPlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                val connected = future.get()
                connected.addListener(playerListener)
                controller = connected
                _player.value = connected
                _currentMediaItemIndex.value = connected.currentMediaItemIndex
                _playWhenReady.value = connected.playWhenReady
                // Subtitles strictly off by default: disable text selection until the user opts in,
                // so forced/default-flagged tracks never appear on their own.
                connected.trackSelectionParameters =
                    connected.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                mapTextTracks(connected.currentTracks)
                pendingCommands.forEach { it(connected) }
                pendingCommands.clear()
                refreshPosterForCurrentItem()
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

private fun labelFor(format: Format): String = format.label ?: format.language ?: "Subtitle"

private fun MediaEntry.toMediaItem(): MediaItem {
    val metadata =
        when (this) {
            is MediaEntry.Audio ->
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            is MediaEntry.Video ->
                // No artworkData here: it is filled in asynchronously by refreshPosterForCurrentItem once
                // a frame is decoded. Never artworkUri — the SAF content uri is not readable by SystemUI.
                MediaMetadata.Builder()
                    .setTitle(video.displayName)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                    .build()
        }
    val builder =
        MediaItem.Builder()
            .setMediaId(uri)
            .setUri(uri.toUri())
            .setMediaMetadata(metadata)
    if (this is MediaEntry.Video && video.externalSubtitles.isNotEmpty()) {
        builder.setSubtitleConfigurations(video.externalSubtitles.map { it.toSubtitleConfiguration() })
    }
    return builder.build()
}

// No selection flags: external tracks stay off until the user picks one (subtitles strictly off).
private fun ExternalSubtitle.toSubtitleConfiguration(): MediaItem.SubtitleConfiguration =
    MediaItem.SubtitleConfiguration
        .Builder(uri.toUri())
        .setMimeType(mimeType)
        .setLanguage(language)
        .setLabel(label)
        .build()
