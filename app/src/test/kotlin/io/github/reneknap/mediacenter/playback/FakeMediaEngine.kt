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

    var loadedTrack: AudioTrack? = null
        private set

    var lastPlayWhenReadyAtLoad: Boolean? = null
        private set

    val loadHistory: MutableList<Pair<AudioTrack, Boolean>> = mutableListOf()

    private var trackEndedListener: (() -> Unit)? = null

    override fun loadTrack(
        track: AudioTrack,
        playWhenReady: Boolean,
    ) {
        loadedTrack = track
        lastPlayWhenReadyAtLoad = playWhenReady
        loadHistory.add(track to playWhenReady)
        _isPlaying.value = playWhenReady
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        _isPlaying.value = playWhenReady
    }

    override fun setOnTrackEndedListener(listener: () -> Unit) {
        trackEndedListener = listener
    }

    fun triggerTrackEnded() {
        trackEndedListener?.invoke()
    }

    fun setPosition(positionMs: Long) {
        _positionMs.value = positionMs
    }

    fun setDuration(durationMs: Long) {
        _durationMs.value = durationMs
    }
}
