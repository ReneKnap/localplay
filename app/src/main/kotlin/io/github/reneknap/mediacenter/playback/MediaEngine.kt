package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import kotlinx.coroutines.flow.StateFlow

interface MediaEngine {
    val isPlaying: StateFlow<Boolean>

    val positionMs: StateFlow<Long>

    val durationMs: StateFlow<Long>

    val currentMediaItemIndex: StateFlow<Int>

    val playWhenReady: StateFlow<Boolean>

    fun setQueue(
        items: List<AudioTrack>,
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
        item: AudioTrack,
    )

    fun seekTo(positionMs: Long)

    fun setPlayWhenReady(playWhenReady: Boolean)
}
