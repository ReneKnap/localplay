package io.github.reneknap.mediacenter.ui.videoplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.video.FolderVideos
import io.github.reneknap.mediacenter.data.video.VideoItem
import io.github.reneknap.mediacenter.data.video.VideoRepository
import io.github.reneknap.mediacenter.data.video.VideoScanState
import io.github.reneknap.mediacenter.playback.VideoPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
        private val videoPlayer: VideoPlayer,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val folderUri: String = savedStateHandle.get<String>(ARG_FOLDER_URI).orEmpty()
        private val startVideoUri: String? = savedStateHandle.get<String>(ARG_START_VIDEO_URI)

        val player: Player?
            get() = videoPlayer.player

        val uiState: StateFlow<VideoPlayerUiState> =
            videoRepository.folders
                .map(::project)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = VideoPlayerUiState.Loading,
                )

        init {
            // One-shot: feed the player the moment the folder first scans ready. The flow completes
            // after the first ready emission, so later re-emits never reload the playlist.
            viewModelScope.launch {
                val videos = videoRepository.folders.mapNotNull(::readyVideosOrNull).first()
                if (videos.isNotEmpty()) {
                    val startIndex = videos.indexOfFirst { it.uri == startVideoUri }.takeIf { it >= 0 } ?: 0
                    videoPlayer.setPlaylist(videos, startIndex)
                }
            }
        }

        fun pause() {
            videoPlayer.pause()
        }

        override fun onCleared() {
            videoPlayer.release()
        }

        private fun readyVideosOrNull(folders: List<FolderVideos>): List<VideoItem>? =
            (folders.firstOrNull { it.folder.uri == folderUri }?.scan as? VideoScanState.Ready)?.videos

        private fun project(folders: List<FolderVideos>): VideoPlayerUiState {
            val target =
                folders.firstOrNull { it.folder.uri == folderUri }
                    ?: return VideoPlayerUiState.NotAvailable
            return when (val scan = target.scan) {
                VideoScanState.Scanning -> VideoPlayerUiState.Loading
                VideoScanState.Unreachable -> VideoPlayerUiState.NotAvailable
                is VideoScanState.Ready ->
                    if (scan.videos.isEmpty()) {
                        VideoPlayerUiState.EmptyFolder(target.folder.displayName)
                    } else {
                        VideoPlayerUiState.Ready(target.folder.displayName, scan.videos)
                    }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val ARG_FOLDER_URI = "folderUri"
            const val ARG_START_VIDEO_URI = "startVideoUri"
        }
    }
