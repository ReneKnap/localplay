package io.github.reneknap.mediacenter.data.audio

sealed interface PlaybackQueueState {
    data object Empty : PlaybackQueueState

    data class Active(
        val tracks: List<AudioTrack>,
        val currentIndex: Int,
        val playbackOrder: List<Int>,
        val deactivated: List<Int> = emptyList(),
        val shuffleEnabled: Boolean = false,
    ) : PlaybackQueueState {
        init {
            // ADR-008: tracks is the immutable folder snapshot (lookup table). Every track index is
            // partitioned into exactly one of two ordered lists: playbackOrder (the active queue the
            // player plays) or deactivated (greyed tracks parked at the bottom, skipped during
            // playback). The current track must be an active one.
            require(tracks.isNotEmpty()) { "Active queue must have tracks" }
            require(playbackOrder.isNotEmpty()) { "Active queue must have at least one active track" }
            val combined = playbackOrder + deactivated
            require(combined.toHashSet().size == combined.size) {
                "a track index must not appear in both the active and deactivated lists, nor twice"
            }
            require(combined.toHashSet() == tracks.indices.toHashSet()) {
                "active and deactivated lists together must cover every track index exactly once"
            }
            require(currentIndex in playbackOrder) {
                "currentIndex $currentIndex must be an active track"
            }
        }

        val current: AudioTrack get() = tracks[currentIndex]

        private val positionInOrder: Int get() = playbackOrder.indexOf(currentIndex)
        val hasNext: Boolean get() = positionInOrder < playbackOrder.lastIndex
        val hasPrevious: Boolean get() = positionInOrder > 0
    }
}
