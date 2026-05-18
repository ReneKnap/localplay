package io.github.reneknap.mediacenter.data.audio

sealed interface PlaybackQueueState {
    data object Empty : PlaybackQueueState

    data class Active(
        val tracks: List<AudioTrack>,
        val currentIndex: Int,
        val playbackOrder: List<Int>,
        val shuffleEnabled: Boolean = false,
    ) : PlaybackQueueState {
        init {
            require(tracks.isNotEmpty()) { "Active queue must have tracks" }
            require(currentIndex in tracks.indices) {
                "currentIndex $currentIndex out of bounds for tracks of size ${tracks.size}"
            }
            require(playbackOrder.size == tracks.size && playbackOrder.toHashSet() == tracks.indices.toHashSet()) {
                "playbackOrder must be a permutation of tracks indices"
            }
        }

        val current: AudioTrack get() = tracks[currentIndex]

        private val positionInOrder: Int get() = playbackOrder.indexOf(currentIndex)
        val hasNext: Boolean get() = positionInOrder < playbackOrder.lastIndex
        val hasPrevious: Boolean get() = positionInOrder > 0
    }
}
