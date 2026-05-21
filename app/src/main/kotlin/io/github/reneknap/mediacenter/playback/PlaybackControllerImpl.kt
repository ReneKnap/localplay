package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.PlaybackQueue
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.data.playback.PlaybackPreferencesDataSource
import io.github.reneknap.mediacenter.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
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
                engine.currentMediaItemIndex.collect { playerPos ->
                    mirrorPlayerPositionToQueue(playerPos)
                }
            }
        }

        override suspend fun prepareFolder(folderUri: String) {
            val current = queue.state.value
            val isSameFolder =
                current is PlaybackQueueState.Active &&
                    current.tracks.firstOrNull()?.folderUri == folderUri
            if (isSameFolder) return
            queue.setQueue(folderUri)
            val persistedShuffle = playbackPreferences.shuffleEnabled.first()
            if (persistedShuffle) {
                queue.setShuffleEnabled(enabled = true)
            }
            val newState = queue.state.value as? PlaybackQueueState.Active ?: return
            engine.setQueue(newState.toOrderedItems(), startIndex = 0, playWhenReady = false, startPositionMs = 0L)
        }

        override fun playAtIndex(index: Int) {
            val state = queue.state.value
            if (state !is PlaybackQueueState.Active || index !in state.tracks.indices) return
            val playerPos = state.playbackOrder.indexOf(index)
            engine.seekToMediaItem(playerPos)
            engine.setPlayWhenReady(true)
        }

        override fun togglePlayPause() {
            engine.setPlayWhenReady(!engine.playWhenReady.value)
        }

        override fun next() {
            engine.seekToNext()
        }

        override fun previous() {
            engine.seekToPrevious()
        }

        override fun seekTo(positionMs: Long) {
            engine.seekTo(positionMs)
        }

        override fun setShuffleEnabled(enabled: Boolean) {
            val state = queue.state.value
            if (state is PlaybackQueueState.Active && state.shuffleEnabled != enabled) {
                queue.setShuffleEnabled(enabled)
                val newState = queue.state.value as PlaybackQueueState.Active
                val startIndex = newState.playbackOrder.indexOf(newState.currentIndex)
                // Keep the current track playing at its position across the reorder (startPositionMs = null).
                engine.setQueue(
                    newState.toOrderedItems(),
                    startIndex,
                    engine.playWhenReady.value,
                    startPositionMs = null,
                )
            }
            scope.launch {
                playbackPreferences.setShuffleEnabled(enabled)
            }
        }

        override fun moveTrack(
            fromPosition: Int,
            toPosition: Int,
        ) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (fromPosition !in state.playbackOrder.indices || toPosition !in state.playbackOrder.indices) return
            if (fromPosition == toPosition) return
            queue.move(fromPosition, toPosition)
            engine.moveMediaItem(fromPosition, toPosition)
        }

        override fun deactivateTrack(position: Int) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (position !in state.playbackOrder.indices) return
            if (state.playbackOrder.size == 1) return // keep at least one active track
            // Mutate the queue first so the engine→queue mirror reconciles the current index against
            // the already-updated playback order when the running track is the one deactivated.
            queue.deactivate(position)
            engine.removeMediaItem(position)
        }

        override fun playTrackNext(position: Int) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (position !in state.playbackOrder.indices) return
            if (position == state.playbackOrder.indexOf(state.currentIndex)) return
            val movedTrackIndex = state.playbackOrder[position]
            queue.moveAfterCurrent(position)
            val targetPosition =
                (queue.state.value as? PlaybackQueueState.Active)?.playbackOrder?.indexOf(movedTrackIndex) ?: return
            engine.moveMediaItem(position, targetPosition)
        }

        override fun reactivateTrack(trackIndex: Int) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (trackIndex !in state.deactivated) return
            queue.reactivate(trackIndex)
            val appendedPosition =
                (queue.state.value as? PlaybackQueueState.Active)?.playbackOrder?.indexOf(trackIndex) ?: return
            engine.addMediaItem(appendedPosition, state.tracks[trackIndex])
        }

        override fun reactivateTrackAt(
            trackIndex: Int,
            position: Int,
        ) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (trackIndex !in state.deactivated) return
            queue.reactivateAt(trackIndex, position)
            val insertedPosition =
                (queue.state.value as? PlaybackQueueState.Active)?.playbackOrder?.indexOf(trackIndex) ?: return
            engine.addMediaItem(insertedPosition, state.tracks[trackIndex])
        }

        override fun resetQueue() {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            queue.reset()
            val newState = queue.state.value as? PlaybackQueueState.Active ?: return
            val startIndex = newState.playbackOrder.indexOf(newState.currentIndex)
            // Re-push the full folder order; keep the current track playing at its position (null).
            engine.setQueue(newState.toOrderedItems(), startIndex, engine.playWhenReady.value, startPositionMs = null)
            scope.launch {
                playbackPreferences.setShuffleEnabled(false)
            }
        }

        private fun mirrorPlayerPositionToQueue(playerPos: Int) {
            val state = queue.state.value as? PlaybackQueueState.Active ?: return
            if (playerPos !in state.playbackOrder.indices) return
            val mirroredIndex = state.playbackOrder[playerPos]
            if (mirroredIndex != state.currentIndex) {
                queue.moveTo(mirroredIndex)
            }
        }

        private fun PlaybackQueueState.Active.toOrderedItems() = playbackOrder.map { tracks[it] }
    }
