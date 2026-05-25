package io.github.reneknap.mediacenter.ui.player

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.audio.ArtworkReader
import io.github.reneknap.mediacenter.data.audio.PlaybackQueue
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.data.media.FolderMediaContent
import io.github.reneknap.mediacenter.data.media.MediaContentScanState
import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.data.media.MediaRepository
import io.github.reneknap.mediacenter.playback.PlaybackController
import io.github.reneknap.mediacenter.playback.PlayerStatus
import io.github.reneknap.mediacenter.playback.SubtitleTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderPlayerViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val queue: PlaybackQueue,
        private val controller: PlaybackController,
        private val artworkReader: ArtworkReader,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val folderUri: String = savedStateHandle.get<String>(ARG_FOLDER_URI).orEmpty()
        private val startEntryUri: String? = savedStateHandle.get<String>(ARG_START_TRACK_URI)

        private val selectedIndex = MutableStateFlow<Int?>(null)
        private val fullscreen = MutableStateFlow(false)

        /** The session [Player] for binding the inline/fullscreen video surface (ADR-010). */
        val player: StateFlow<Player?> get() = controller.player

        // Pre-combined so the main projection stays within combine's five-argument form.
        private val playbackSnapshot =
            combine(
                controller.status,
                controller.textTracks,
                controller.activeTextTrackId,
            ) { status, tracks, activeId ->
                PlaybackSnapshot(status, tracks, activeId)
            }

        val uiState: StateFlow<FolderPlayerUiState> =
            combine(
                mediaRepository.folders,
                queue.state,
                playbackSnapshot,
                selectedIndex,
                fullscreen,
            ) { folders, queueState, snapshot, selected, isFullscreen ->
                project(folders, queueState, snapshot, selected, isFullscreen)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = FolderPlayerUiState.Loading,
            )

        init {
            viewModelScope.launch {
                controller.prepareFolder(folderUri)
                startEntryUri?.let { applyStartEntrySelection(it) }
            }
            viewModelScope.launch {
                // Leave fullscreen automatically once the current entry is no longer a video, so a later
                // video (auto-advance or manual) never silently re-enters fullscreen.
                queue.state.collect { state ->
                    val current = (state as? PlaybackQueueState.Active)?.let { it.entries[it.currentIndex] }
                    if (current !is MediaEntry.Video) fullscreen.value = false
                }
            }
        }

        private suspend fun applyStartEntrySelection(entryUri: String) {
            val entries = readyEntries() ?: return
            val index = entries.indexOfFirst { it.uri == entryUri }
            if (index >= 0) selectedIndex.value = index
        }

        private suspend fun readyEntries(): List<MediaEntry>? {
            val folders = mediaRepository.folders.first()
            val scan = folders.firstOrNull { it.folder.uri == folderUri }?.scan
            return (scan as? MediaContentScanState.Ready)?.entries
        }

        fun selectTrack(index: Int) {
            val isPlaying = controller.status.value.isPlaying
            if (isPlaying) {
                val currentIdx = currentIndexFor(queue.state.value)
                if (currentIdx != index) {
                    controller.playAtIndex(index)
                    selectedIndex.value = null
                }
                return
            }
            selectedIndex.update { current -> if (current == index) null else index }
        }

        fun play() {
            controller.playAtIndex(selectedIndex.value ?: 0)
            selectedIndex.value = null
        }

        fun togglePlayPause() {
            controller.togglePlayPause()
        }

        fun next() {
            controller.next()
        }

        fun previous() {
            controller.previous()
        }

        fun seekTo(positionMs: Long) {
            controller.seekTo(positionMs)
        }

        suspend fun artworkFor(uri: String): Bitmap? = artworkReader.loadArtwork(uri)

        suspend fun thumbnailFor(uri: String): Bitmap? = artworkReader.loadArtwork(uri, ROW_THUMBNAIL_SIZE_PX)

        fun moveTrack(
            fromPosition: Int,
            toPosition: Int,
        ) {
            controller.moveTrack(fromPosition, toPosition)
        }

        fun deactivateTrack(position: Int) {
            val active = queue.state.value as? PlaybackQueueState.Active ?: return
            val entryIndex = active.playbackOrder.getOrNull(position) ?: return
            if (selectedIndex.value == entryIndex) selectedIndex.value = null
            controller.deactivateTrack(position)
        }

        fun playTrackNext(position: Int) {
            controller.playTrackNext(position)
        }

        fun reactivateTrack(trackIndex: Int) {
            controller.reactivateTrack(trackIndex)
        }

        fun reactivateTrackAt(
            trackIndex: Int,
            position: Int,
        ) {
            controller.reactivateTrackAt(trackIndex, position)
        }

        fun resetQueue() {
            controller.resetQueue()
        }

        fun selectSubtitleTrack(id: String) {
            controller.selectTextTrack(id)
        }

        fun disableSubtitles() {
            controller.disableSubtitles()
        }

        fun toggleFullscreen() {
            val current = (queue.state.value as? PlaybackQueueState.Active)?.let { it.entries[it.currentIndex] }
            if (current is MediaEntry.Video) {
                fullscreen.update { !it }
            } else {
                fullscreen.value = false
            }
        }

        fun toggleShuffle() {
            val enabling = !controller.status.value.shuffleEnabled
            if (enabling) {
                selectedIndex.value?.let { queue.moveTo(it) }
            }
            controller.setShuffleEnabled(enabling)
        }

        private fun project(
            folders: List<FolderMediaContent>,
            queueState: PlaybackQueueState,
            snapshot: PlaybackSnapshot,
            selected: Int?,
            isFullscreen: Boolean,
        ): FolderPlayerUiState {
            val target = folders.firstOrNull { it.folder.uri == folderUri } ?: return FolderPlayerUiState.NotAvailable
            return when (val scan = target.scan) {
                MediaContentScanState.Scanning -> FolderPlayerUiState.Loading
                MediaContentScanState.Unreachable -> FolderPlayerUiState.NotAvailable
                is MediaContentScanState.Ready ->
                    if (scan.entries.isEmpty()) {
                        FolderPlayerUiState.EmptyFolder(target.folder.displayName)
                    } else {
                        val active = activeForFolder(queueState)
                        val currentEntry = active?.let { it.entries[it.currentIndex] }
                        val isCurrentVideo = currentEntry is MediaEntry.Video
                        FolderPlayerUiState.Ready(
                            folderName = target.folder.displayName,
                            entries = scan.entries,
                            displayOrder = active?.playbackOrder ?: scan.entries.indices.toList(),
                            deactivatedOrder = active?.deactivated ?: emptyList(),
                            currentIndex = active?.currentIndex,
                            selectedIndex = selected,
                            status = snapshot.status,
                            shuffleEnabled = snapshot.status.shuffleEnabled,
                            hasNext = active?.hasNext ?: false,
                            hasPrevious = active?.hasPrevious ?: false,
                            isCurrentVideo = isCurrentVideo,
                            isFullscreen = isFullscreen && isCurrentVideo,
                            // Subtitles are a video-only concern; hide tracks while audio is current.
                            subtitleTracks = if (isCurrentVideo) snapshot.subtitleTracks else emptyList(),
                            activeSubtitleTrackId = if (isCurrentVideo) snapshot.activeSubtitleTrackId else null,
                        )
                    }
            }
        }

        private data class PlaybackSnapshot(
            val status: PlayerStatus,
            val subtitleTracks: List<SubtitleTrack>,
            val activeSubtitleTrackId: String?,
        )

        private fun currentIndexFor(queueState: PlaybackQueueState): Int? = activeForFolder(queueState)?.currentIndex

        private fun activeForFolder(queueState: PlaybackQueueState): PlaybackQueueState.Active? {
            val active = queueState as? PlaybackQueueState.Active ?: return null
            if (active.entries.firstOrNull()?.folderUri != folderUri) return null
            return active
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val ARG_FOLDER_URI = "folderUri"
            const val ARG_START_TRACK_URI = "startTrackUri"
            const val ROW_THUMBNAIL_SIZE_PX = 128
        }
    }
