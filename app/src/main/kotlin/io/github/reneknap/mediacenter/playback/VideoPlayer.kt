package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import io.github.reneknap.mediacenter.data.video.VideoItem

/**
 * Screen-scoped video player wrapper (ADR-009). Distinct from the audio
 * [MediaCenterPlaybackService]/MediaSession — foreground-only, no notification.
 *
 * [player] is the underlying Media3 [Player] for binding into a `PlayerView`; it stays a thin
 * boundary so [io.github.reneknap.mediacenter.ui.videoplayer.VideoPlayerViewModel] is unit-testable
 * with a fake.
 */
interface VideoPlayer {
    val player: Player?

    fun setPlaylist(
        items: List<VideoItem>,
        startIndex: Int,
    )

    fun pause()

    fun release()
}
