package io.github.reneknap.mediacenter.playback

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val status: StateFlow<PlayerStatus>

    suspend fun prepareFolder(folderUri: String)

    fun playAtIndex(index: Int)

    fun togglePlayPause()

    fun next()

    fun previous()

    fun setShuffleEnabled(enabled: Boolean)
}
