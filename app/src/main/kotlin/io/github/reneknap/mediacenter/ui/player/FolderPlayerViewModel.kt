package io.github.reneknap.mediacenter.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.audio.AudioRepository
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.audio.PlaybackQueue
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.playback.PlaybackController
import io.github.reneknap.mediacenter.playback.PlayerStatus
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
        private val audioRepository: AudioRepository,
        private val queue: PlaybackQueue,
        private val controller: PlaybackController,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val folderUri: String = savedStateHandle.get<String>(ARG_FOLDER_URI).orEmpty()
        private val startTrackUri: String? = savedStateHandle.get<String>(ARG_START_TRACK_URI)

        private val selectedIndex = MutableStateFlow<Int?>(null)

        val uiState: StateFlow<FolderPlayerUiState> =
            combine(
                audioRepository.folders,
                queue.state,
                controller.status,
                selectedIndex,
            ) { folders, queueState, status, selected ->
                project(folders, queueState, status, selected)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = FolderPlayerUiState.Loading,
            )

        init {
            viewModelScope.launch {
                controller.prepareFolder(folderUri)
                startTrackUri?.let { applyStartTrackSelection(it) }
            }
        }

        private suspend fun applyStartTrackSelection(trackUri: String) {
            val tracks = readyTracks() ?: return
            val index = tracks.indexOfFirst { it.uri == trackUri }
            if (index >= 0) selectedIndex.value = index
        }

        private suspend fun readyTracks(): List<AudioTrack>? {
            val folders = audioRepository.folders.first()
            val scan = folders.firstOrNull { it.folder.uri == folderUri }?.scan
            return (scan as? FolderScanState.Ready)?.tracks
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

        fun toggleShuffle() {
            val enabling = !controller.status.value.shuffleEnabled
            if (enabling) {
                selectedIndex.value?.let { queue.moveTo(it) }
            }
            controller.setShuffleEnabled(enabling)
        }

        private fun project(
            folders: List<FolderTracks>,
            queueState: PlaybackQueueState,
            status: PlayerStatus,
            selected: Int?,
        ): FolderPlayerUiState {
            val target = folders.firstOrNull { it.folder.uri == folderUri } ?: return FolderPlayerUiState.NotAvailable
            return when (val scan = target.scan) {
                FolderScanState.Scanning -> FolderPlayerUiState.Loading
                FolderScanState.Unreachable -> FolderPlayerUiState.NotAvailable
                is FolderScanState.Ready -> {
                    val active = activeForFolder(queueState)
                    FolderPlayerUiState.Ready(
                        folderName = target.folder.displayName,
                        tracks = scan.tracks,
                        displayOrder = active?.playbackOrder ?: scan.tracks.indices.toList(),
                        currentIndex = active?.currentIndex,
                        selectedIndex = selected,
                        status = status,
                        shuffleEnabled = status.shuffleEnabled,
                        hasNext = active?.hasNext ?: false,
                        hasPrevious = active?.hasPrevious ?: false,
                    )
                }
            }
        }

        private fun currentIndexFor(queueState: PlaybackQueueState): Int? =
            activeForFolder(queueState)?.currentIndex

        private fun activeForFolder(queueState: PlaybackQueueState): PlaybackQueueState.Active? {
            val active = queueState as? PlaybackQueueState.Active ?: return null
            if (active.tracks.firstOrNull()?.folderUri != folderUri) return null
            return active
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val ARG_FOLDER_URI = "folderUri"
            const val ARG_START_TRACK_URI = "startTrackUri"
        }
    }
