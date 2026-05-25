package io.github.reneknap.mediacenter.playback

/**
 * A selectable subtitle/text track surfaced from the player (ADR-011). [id] is opaque and only valid
 * for the current track set (it is rebuilt whenever the player's tracks change); [language] is the
 * track's language tag when the source provides one.
 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String?,
)
