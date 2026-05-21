package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.flow.StateFlow

interface PlaybackQueue {
    val state: StateFlow<PlaybackQueueState>

    suspend fun setQueue(
        folderUri: String,
        startTrackUri: String? = null,
    )

    fun moveToNext()

    fun moveToPrevious()

    fun moveTo(index: Int)

    fun clear()

    fun setShuffleEnabled(enabled: Boolean)

    /** Reorders the active queue: moves the entry at [fromPosition] to [toPosition] (positions in playbackOrder). */
    fun move(
        fromPosition: Int,
        toPosition: Int,
    )

    /**
     * Deactivates the active entry at [position]: removes it from playbackOrder and parks it at the
     * back of the deactivated list (greyed, skipped during playback). No-op for the only remaining
     * active track — the queue always keeps at least one active track.
     */
    fun deactivate(position: Int)

    /** Moves the active entry at [position] to directly after the current track in playback order. */
    fun moveAfterCurrent(position: Int)

    /** Reactivates a deactivated [trackIndex]: removes it from the deactivated list and appends it to the active queue. */
    fun reactivate(trackIndex: Int)

    /** Reactivates a deactivated [trackIndex] by inserting it into the active queue at [position] (drag-drop). */
    fun reactivateAt(
        trackIndex: Int,
        position: Int,
    )

    /** Resets the queue to all tracks in natural folder order, clears the deactivated list, disables shuffle. */
    fun reset()
}
