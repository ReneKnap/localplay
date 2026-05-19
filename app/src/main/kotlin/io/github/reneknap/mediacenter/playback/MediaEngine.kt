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
    )

    fun seekToNext()

    fun seekToPrevious()

    fun seekToMediaItem(index: Int)

    fun setPlayWhenReady(playWhenReady: Boolean)
}
