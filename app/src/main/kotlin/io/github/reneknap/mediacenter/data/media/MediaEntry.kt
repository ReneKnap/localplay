package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.video.VideoItem

/**
 * Unified queue/list element across both media kinds (ADR-010). Wraps the rich, kind-specific models
 * ([AudioTrack] tags, [VideoItem] resolution) behind a common contract so the queue, engine and UI
 * operate on one heterogeneous list.
 */
sealed interface MediaEntry {
    val uri: String
    val folderUri: String
    val displayName: String
    val durationMs: Long
    val kind: MediaKind

    data class Audio(val track: AudioTrack) : MediaEntry {
        override val uri: String get() = track.uri
        override val folderUri: String get() = track.folderUri
        override val displayName: String get() = track.displayName
        override val durationMs: Long get() = track.durationMs
        override val kind: MediaKind get() = MediaKind.AUDIO
    }

    data class Video(val video: VideoItem) : MediaEntry {
        override val uri: String get() = video.uri
        override val folderUri: String get() = video.folderUri
        override val displayName: String get() = video.displayName
        override val durationMs: Long get() = video.durationMs
        override val kind: MediaKind get() = MediaKind.VIDEO
    }
}
