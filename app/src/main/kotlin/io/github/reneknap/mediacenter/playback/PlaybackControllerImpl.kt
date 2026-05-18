package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.PlaybackQueue
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.data.playback.PlaybackPreferencesDataSource
import io.github.reneknap.mediacenter.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackControllerImpl
    @Inject
    constructor(
        private val queue: PlaybackQueue,
        private val engine: MediaEngine,
        private val playbackPreferences: PlaybackPreferencesDataSource,
        @ApplicationScope private val scope: CoroutineScope,
    ) : PlaybackController {
        private val playIntent = MutableStateFlow(false)
        private var lastLoadedUri: String? = null

        override val status: StateFlow<PlayerStatus> =
            combine(
                engine.isPlaying,
                engine.positionMs,
                engine.durationMs,
                playbackPreferences.shuffleEnabled,
            ) { playing, position, duration, shuffle ->
                PlayerStatus(
                    isPlaying = playing,
                    positionMs = position,
                    durationMs = duration,
                    shuffleEnabled = shuffle,
                )
            }.stateIn(scope, SharingStarted.Eagerly, PlayerStatus())

        init {
            scope.launch {
                queue.state.collect { state ->
                    if (state is PlaybackQueueState.Active) {
                        val track = state.current
                        if (track.uri != lastLoadedUri) {
                            lastLoadedUri = track.uri
                            engine.loadTrack(track, playWhenReady = playIntent.value)
                        }
                    }
                }
            }
            engine.setOnTrackEndedListener {
                val current = queue.state.value
                if (current is PlaybackQueueState.Active && current.hasNext) {
                    queue.moveToNext()
                } else {
                    playIntent.value = false
                    engine.setPlayWhenReady(false)
                }
            }
        }

        override suspend fun prepareFolder(folderUri: String) {
            val current = queue.state.value
            val isSameFolder =
                current is PlaybackQueueState.Active &&
                    current.tracks.firstOrNull()?.folderUri == folderUri
            if (isSameFolder) return
            playIntent.value = false
            queue.setQueue(folderUri)
            val persistedShuffle = playbackPreferences.shuffleEnabled.first()
            if (persistedShuffle) {
                queue.setShuffleEnabled(enabled = true)
            }
        }

        override fun playAtIndex(index: Int) {
            val state = queue.state.value
            if (state !is PlaybackQueueState.Active || index !in state.tracks.indices) return
            playIntent.value = true
            queue.moveTo(index)
            engine.setPlayWhenReady(true)
        }

        override fun togglePlayPause() {
            val newValue = !playIntent.value
            playIntent.value = newValue
            engine.setPlayWhenReady(newValue)
        }

        override fun next() {
            queue.moveToNext()
        }

        override fun previous() {
            queue.moveToPrevious()
        }

        override fun setShuffleEnabled(enabled: Boolean) {
            queue.setShuffleEnabled(enabled)
            scope.launch {
                playbackPreferences.setShuffleEnabled(enabled)
            }
        }
    }
