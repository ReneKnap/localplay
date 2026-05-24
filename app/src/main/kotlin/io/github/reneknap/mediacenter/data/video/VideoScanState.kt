package io.github.reneknap.mediacenter.data.video

sealed interface VideoScanState {
    data object Scanning : VideoScanState

    data class Ready(val videos: List<VideoItem>) : VideoScanState

    data object Unreachable : VideoScanState
}
