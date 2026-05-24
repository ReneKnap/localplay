package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import io.github.reneknap.mediacenter.data.video.VideoItem

class FakeVideoPlayer : VideoPlayer {
    // Rendering is not unit-tested, so the real Player is never needed.
    override val player: Player? = null

    val setPlaylistCalls = mutableListOf<Pair<List<VideoItem>, Int>>()

    var pauseCount = 0
        private set

    var releaseCount = 0
        private set

    override fun setPlaylist(
        items: List<VideoItem>,
        startIndex: Int,
    ) {
        setPlaylistCalls += items to startIndex
    }

    override fun pause() {
        pauseCount++
    }

    override fun release() {
        releaseCount++
    }
}
