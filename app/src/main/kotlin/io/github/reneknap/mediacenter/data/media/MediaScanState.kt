package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.video.VideoItem

sealed interface MediaScanState {
    data object Scanning : MediaScanState

    data class Ready(
        val audio: List<AudioTrack>,
        val video: List<VideoItem>,
    ) : MediaScanState

    data object Unreachable : MediaScanState
}
