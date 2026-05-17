package io.github.reneknap.mediacenter.playback

data class PlayerStatus(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)
