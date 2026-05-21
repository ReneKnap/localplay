package io.github.reneknap.mediacenter.playback

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val status: StateFlow<PlayerStatus>

    suspend fun prepareFolder(folderUri: String)

    fun playAtIndex(index: Int)

    fun togglePlayPause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun setShuffleEnabled(enabled: Boolean)

    fun moveTrack(
        fromPosition: Int,
        toPosition: Int,
    )

    fun deactivateTrack(position: Int)

    fun playTrackNext(position: Int)

    fun reactivateTrack(trackIndex: Int)

    fun reactivateTrackAt(
        trackIndex: Int,
        position: Int,
    )

    fun resetQueue()
}
