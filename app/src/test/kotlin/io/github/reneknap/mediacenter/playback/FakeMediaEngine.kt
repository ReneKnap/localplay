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
        val startPositionMs: Long?,
    )

    sealed interface Seek {
        data object Next : Seek

        data object Previous : Seek

        data class MediaItem(val index: Int) : Seek

        data class Position(val positionMs: Long) : Seek
    }

    sealed interface Structural {
        data class Move(val fromIndex: Int, val toIndex: Int) : Structural

        data class Remove(val index: Int) : Structural

        data class Add(val index: Int, val item: AudioTrack) : Structural
    }

    val setQueueHistory: MutableList<SetQueueCall> = mutableListOf()
    val seekHistory: MutableList<Seek> = mutableListOf()
    val structuralHistory: MutableList<Structural> = mutableListOf()

    override fun setQueue(
        items: List<AudioTrack>,
        startIndex: Int,
        playWhenReady: Boolean,
        startPositionMs: Long?,
    ) {
        this.items = items
        _currentMediaItemIndex.value = startIndex
        _playWhenReady.value = playWhenReady
        _isPlaying.value = playWhenReady
        if (startPositionMs != null) _positionMs.value = startPositionMs
        setQueueHistory.add(SetQueueCall(items, startIndex, playWhenReady, startPositionMs))
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

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        structuralHistory.add(Structural.Move(fromIndex, toIndex))
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        val current = items.getOrNull(_currentMediaItemIndex.value)
        val mutable = items.toMutableList()
        mutable.add(toIndex, mutable.removeAt(fromIndex))
        items = mutable
        // moveMediaItem keeps the current item identity; only its index may change.
        if (current != null) _currentMediaItemIndex.value = mutable.indexOf(current)
    }

    override fun removeMediaItem(index: Int) {
        structuralHistory.add(Structural.Remove(index))
        if (index !in items.indices) return
        val current = items.getOrNull(_currentMediaItemIndex.value)
        val mutable = items.toMutableList()
        mutable.removeAt(index)
        items = mutable
        // Mirrors Media3 auto-advance: if the current item was removed, the item that shifted into
        // its slot becomes current (clamped to the last index when the removed item was last).
        _currentMediaItemIndex.value =
            when {
                mutable.isEmpty() -> 0
                current != null && current in mutable -> mutable.indexOf(current)
                else -> index.coerceAtMost(mutable.lastIndex)
            }
    }

    override fun addMediaItem(
        index: Int,
        item: AudioTrack,
    ) {
        structuralHistory.add(Structural.Add(index, item))
        val current = items.getOrNull(_currentMediaItemIndex.value)
        val mutable = items.toMutableList()
        mutable.add(index.coerceIn(0, mutable.size), item)
        items = mutable
        if (current != null) _currentMediaItemIndex.value = mutable.indexOf(current)
    }

    override fun seekTo(positionMs: Long) {
        seekHistory.add(Seek.Position(positionMs))
        _positionMs.value = positionMs
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
