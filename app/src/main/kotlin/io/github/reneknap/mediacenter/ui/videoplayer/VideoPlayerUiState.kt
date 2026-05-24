package io.github.reneknap.mediacenter.ui.videoplayer

import io.github.reneknap.mediacenter.data.video.VideoItem

sealed interface VideoPlayerUiState {
    data object Loading : VideoPlayerUiState

    data object NotAvailable : VideoPlayerUiState

    data class EmptyFolder(val folderName: String) : VideoPlayerUiState

    data class Ready(
        val folderName: String,
        val videos: List<VideoItem>,
    ) : VideoPlayerUiState
}
