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
}
