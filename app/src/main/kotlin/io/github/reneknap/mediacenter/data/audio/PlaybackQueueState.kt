package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.media.MediaEntry

sealed interface PlaybackQueueState {
    data object Empty : PlaybackQueueState

    data class Active(
        val entries: List<MediaEntry>,
        val currentIndex: Int,
        val playbackOrder: List<Int>,
        val deactivated: List<Int> = emptyList(),
        val shuffleEnabled: Boolean = false,
    ) : PlaybackQueueState {
        init {
            // ADR-008/ADR-010: entries is the immutable folder snapshot (lookup table) over both media
            // kinds. Every entry index is partitioned into exactly one of two ordered lists:
            // playbackOrder (the active queue the player plays) or deactivated (greyed entries parked
            // at the bottom, skipped during playback). The current entry must be an active one.
            require(entries.isNotEmpty()) { "Active queue must have entries" }
            require(playbackOrder.isNotEmpty()) { "Active queue must have at least one active entry" }
            val combined = playbackOrder + deactivated
            require(combined.toHashSet().size == combined.size) {
                "an entry index must not appear in both the active and deactivated lists, nor twice"
            }
            require(combined.toHashSet() == entries.indices.toHashSet()) {
                "active and deactivated lists together must cover every entry index exactly once"
            }
            require(currentIndex in playbackOrder) {
                "currentIndex $currentIndex must be an active entry"
            }
        }

        val current: MediaEntry get() = entries[currentIndex]

        private val positionInOrder: Int get() = playbackOrder.indexOf(currentIndex)
        val hasNext: Boolean get() = positionInOrder < playbackOrder.lastIndex
        val hasPrevious: Boolean get() = positionInOrder > 0
    }
}
