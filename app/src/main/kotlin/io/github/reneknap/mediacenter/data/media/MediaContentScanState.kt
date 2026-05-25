package io.github.reneknap.mediacenter.data.media

sealed interface MediaContentScanState {
    data object Scanning : MediaContentScanState

    data class Ready(val entries: List<MediaEntry>) : MediaContentScanState

    data object Unreachable : MediaContentScanState
}
