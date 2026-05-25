package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import io.github.reneknap.mediacenter.data.media.MediaEntry
import kotlinx.coroutines.flow.StateFlow

interface MediaEngine {
    /** The underlying Media3 [Player] for binding a video surface (`PlayerView`); null until connected. */
    val player: StateFlow<Player?>

    val isPlaying: StateFlow<Boolean>

    val positionMs: StateFlow<Long>

    val durationMs: StateFlow<Long>

    val currentMediaItemIndex: StateFlow<Int>

    val playWhenReady: StateFlow<Boolean>

    fun setQueue(
        items: List<MediaEntry>,
        startIndex: Int,
        playWhenReady: Boolean,
        startPositionMs: Long?,
    )

    fun seekToNext()

    fun seekToPrevious()

    fun seekToMediaItem(index: Int)

    fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    )

    fun removeMediaItem(index: Int)

    fun addMediaItem(
        index: Int,
        item: MediaEntry,
    )

    fun seekTo(positionMs: Long)

    fun setPlayWhenReady(playWhenReady: Boolean)
}
