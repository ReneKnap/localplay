package io.github.reneknap.mediacenter.playback

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlaybackController : PlaybackController {
    private val _status = MutableStateFlow(PlayerStatus())
    override val status: StateFlow<PlayerStatus> = _status.asStateFlow()

    override val player: StateFlow<Player?> = MutableStateFlow<Player?>(null).asStateFlow()

    val preparedFolders: MutableList<String> = mutableListOf()
    val playedIndexes: MutableList<Int> = mutableListOf()
    val seekedPositions: MutableList<Long> = mutableListOf()
    val shuffleEnabledCalls: MutableList<Boolean> = mutableListOf()
    val movedTracks: MutableList<Pair<Int, Int>> = mutableListOf()
    val deactivatedPositions: MutableList<Int> = mutableListOf()
    val playNextPositions: MutableList<Int> = mutableListOf()
    val reactivatedTracks: MutableList<Int> = mutableListOf()
    val reactivatedAtTracks: MutableList<Pair<Int, Int>> = mutableListOf()
    var resetQueueCount: Int = 0
        private set
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

    override fun moveTrack(
        fromPosition: Int,
        toPosition: Int,
    ) {
        movedTracks.add(fromPosition to toPosition)
    }

    override fun deactivateTrack(position: Int) {
        deactivatedPositions.add(position)
    }

    override fun playTrackNext(position: Int) {
        playNextPositions.add(position)
    }

    override fun reactivateTrack(trackIndex: Int) {
        reactivatedTracks.add(trackIndex)
    }

    override fun reactivateTrackAt(
        trackIndex: Int,
        position: Int,
    ) {
        reactivatedAtTracks.add(trackIndex to position)
    }

    override fun resetQueue() {
        resetQueueCount += 1
    }

    fun emitStatus(status: PlayerStatus) {
        _status.value = status
    }
}
