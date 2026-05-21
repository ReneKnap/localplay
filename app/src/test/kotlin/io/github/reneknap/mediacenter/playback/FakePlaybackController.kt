package io.github.reneknap.mediacenter.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlaybackController : PlaybackController {
    private val _status = MutableStateFlow(PlayerStatus())
    override val status: StateFlow<PlayerStatus> = _status.asStateFlow()

    val preparedFolders: MutableList<String> = mutableListOf()
    val playedIndexes: MutableList<Int> = mutableListOf()
    val seekedPositions: MutableList<Long> = mutableListOf()
    val shuffleEnabledCalls: MutableList<Boolean> = mutableListOf()
    var togglePlayPauseCount: Int = 0
        private set
    var nextCount: Int = 0
        private set
    var previousCount: Int = 0
        private set

    override suspend fun prepareFolder(folderUri: String) {
        preparedFolders.add(folderUri)
    }

    override fun playAtIndex(index: Int) {
        playedIndexes.add(index)
    }

    override fun togglePlayPause() {
        togglePlayPauseCount += 1
    }

    override fun next() {
        nextCount += 1
    }

    override fun previous() {
        previousCount += 1
    }

    override fun seekTo(positionMs: Long) {
        seekedPositions.add(positionMs)
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabledCalls.add(enabled)
    }

    fun emitStatus(status: PlayerStatus) {
        _status.value = status
    }
}
