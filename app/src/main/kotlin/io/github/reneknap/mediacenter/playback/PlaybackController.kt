package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val status: StateFlow<PlayerStatus>

    /** The underlying Media3 [Player] for binding a video surface; null until the session connects. */
    val player: StateFlow<Player?>

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
