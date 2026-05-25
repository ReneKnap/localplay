package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val status: StateFlow<PlayerStatus>

    /** The underlying Media3 [Player] for binding a video surface; null until the session connects. */
    val player: StateFlow<Player?>

    /** Selectable subtitle tracks of the current item; empty for none. */
    val textTracks: StateFlow<List<SubtitleTrack>>

    /** The currently shown subtitle track id, or null when subtitles are off (the default). */
    val activeTextTrackId: StateFlow<String?>

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

    /** Show the subtitle track with [id] (from [textTracks]). */
    fun selectTextTrack(id: String)

    /** Turn subtitles off. */
    fun disableSubtitles()
}
