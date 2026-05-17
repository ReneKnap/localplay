package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import kotlinx.coroutines.flow.StateFlow

interface MediaEngine {
    val isPlaying: StateFlow<Boolean>

    val positionMs: StateFlow<Long>

    val durationMs: StateFlow<Long>

    fun loadTrack(
        track: AudioTrack,
        playWhenReady: Boolean,
    )

    fun setPlayWhenReady(playWhenReady: Boolean)

    fun setOnTrackEndedListener(listener: () -> Unit)
}
