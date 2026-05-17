package io.github.reneknap.mediacenter.data.audio

sealed interface FolderScanState {
    data object Scanning : FolderScanState

    data class Ready(val tracks: List<AudioTrack>) : FolderScanState

    data object Unreachable : FolderScanState
}
