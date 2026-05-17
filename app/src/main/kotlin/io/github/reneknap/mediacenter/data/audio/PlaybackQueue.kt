package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.flow.StateFlow

interface PlaybackQueue {
    val state: StateFlow<PlaybackQueueState>

    suspend fun setQueue(folderUri: String)

    fun moveToNext()

    fun moveToPrevious()

    fun clear()
}
