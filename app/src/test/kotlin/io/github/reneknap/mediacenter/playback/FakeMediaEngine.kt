package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMediaEngine : MediaEngine {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(0)
    override val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val _playWhenReady = MutableStateFlow(false)
    override val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()

    var items: List<AudioTrack> = emptyList()
        private set

    data class SetQueueCall(
        val items: List<AudioTrack>,
        val startIndex: Int,
        val playWhenReady: Boolean,
    )

    sealed interface Seek {
        data object Next : Seek

        data object Previous : Seek

        data class MediaItem(val index: Int) : Seek
    }

    val setQueueHistory: MutableList<SetQueueCall> = mutableListOf()
    val seekHistory: MutableList<Seek> = mutableListOf()

    override fun setQueue(
        items: List<AudioTrack>,
        startIndex: Int,
        playWhenReady: Boolean,
    ) {
        this.items = items
        _currentMediaItemIndex.value = startIndex
        _playWhenReady.value = playWhenReady
        _isPlaying.value = playWhenReady
        setQueueHistory.add(SetQueueCall(items, startIndex, playWhenReady))
    }

    override fun seekToNext() {
        seekHistory.add(Seek.Next)
        val next = _currentMediaItemIndex.value + 1
        if (next in items.indices) {
            _currentMediaItemIndex.value = next
        }
    }

    override fun seekToPrevious() {
        seekHistory.add(Seek.Previous)
        val prev = _currentMediaItemIndex.value - 1
        if (prev in items.indices) {
            _currentMediaItemIndex.value = prev
        }
    }

    override fun seekToMediaItem(index: Int) {
        seekHistory.add(Seek.MediaItem(index))
        if (index in items.indices) {
            _currentMediaItemIndex.value = index
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        _playWhenReady.value = playWhenReady
        _isPlaying.value = playWhenReady
    }

    fun simulateAutoAdvance() {
        val next = _currentMediaItemIndex.value + 1
        if (next in items.indices) {
            _currentMediaItemIndex.value = next
        }
    }

    fun setPosition(positionMs: Long) {
        _positionMs.value = positionMs
    }

    fun setDuration(durationMs: Long) {
        _durationMs.value = durationMs
    }
}
