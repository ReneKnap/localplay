package io.github.reneknap.mediacenter.data.audio

sealed interface PlaybackQueueState {
    data object Empty : PlaybackQueueState

    data class Active(
        val tracks: List<AudioTrack>,
        val currentIndex: Int,
    ) : PlaybackQueueState {
        init {
            require(tracks.isNotEmpty()) { "Active queue must have tracks" }
            require(currentIndex in tracks.indices) {
                "currentIndex $currentIndex out of bounds for tracks of size ${tracks.size}"
            }
        }

        val current: AudioTrack get() = tracks[currentIndex]
        val hasNext: Boolean get() = currentIndex < tracks.lastIndex
        val hasPrevious: Boolean get() = currentIndex > 0
    }
}
