package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import io.github.reneknap.mediacenter.data.media.MediaEntry
import kotlinx.coroutines.flow.StateFlow

interface MediaEngine {
    /** The underlying Media3 [Player] for binding a video surface (`PlayerView`); null until connected. */
    val player: StateFlow<Player?>

    val isPlaying: StateFlow<Boolean>

    val positionMs: StateFlow<Long>

    val durationMs: StateFlow<Long>

    val currentMediaItemIndex: StateFlow<Int>

    val playWhenReady: StateFlow<Boolean>

    /** Selectable subtitle/text tracks of the current item (embedded + attached external); empty for none. */
    val textTracks: StateFlow<List<SubtitleTrack>>

    /** The currently shown subtitle track id, or null when subtitles are off (the default). */
    val activeTextTrackId: StateFlow<String?>

    fun setQueue(
        items: List<MediaEntry>,
        startIndex: Int,
        playWhenReady: Boolean,
        startPositionMs: Long?,
    )

    fun seekToNext()

    fun seekToPrevious()

    fun seekToMediaItem(index: Int)

    fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    )

    fun removeMediaItem(index: Int)

    fun addMediaItem(
        index: Int,
        item: MediaEntry,
    )

    fun seekTo(positionMs: Long)

    fun setPlayWhenReady(playWhenReady: Boolean)

    /** Show the subtitle track with [id] (from [textTracks]); no-op if it is no longer present. */
    fun selectTextTrack(id: String)

    /** Turn subtitles off for the current and subsequent items. */
    fun disableSubtitles()
}
