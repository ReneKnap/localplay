package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.media.MediaContentScanState
import io.github.reneknap.mediacenter.data.media.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Snapshot: the active queue is frozen at [setQueue] time; later [MediaRepository.folders]
 * emissions for the same folder do not mutate it.
 */
@Singleton
class PlaybackQueueImpl
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val random: Random = Random.Default,
    ) : PlaybackQueue {
        private val _state = MutableStateFlow<PlaybackQueueState>(PlaybackQueueState.Empty)
        override val state: StateFlow<PlaybackQueueState> = _state.asStateFlow()

        override suspend fun setQueue(
            folderUri: String,
            startTrackUri: String?,
        ) {
            val folders = mediaRepository.folders.first()
            val scan = folders.firstOrNull { it.folder.uri == folderUri }?.scan
            if (scan !is MediaContentScanState.Ready || scan.entries.isEmpty()) {
                _state.value = PlaybackQueueState.Empty
                return
            }
            val startIndex = scan.entries.indexOfFirst { it.uri == startTrackUri }.takeIf { it >= 0 } ?: 0
            _state.value =
                PlaybackQueueState.Active(
                    entries = scan.entries,
                    currentIndex = startIndex,
                    playbackOrder = scan.entries.indices.toList(),
                    shuffleEnabled = false,
                )
        }

        override fun moveToNext() {
            _state.update { current ->
                if (current is PlaybackQueueState.Active && current.hasNext) {
                    val position = current.playbackOrder.indexOf(current.currentIndex)
                    current.copy(currentIndex = current.playbackOrder[position + 1])
                } else {
                    current
                }
            }
        }

        override fun moveToPrevious() {
            _state.update { current ->
                if (current is PlaybackQueueState.Active && current.hasPrevious) {
                    val position = current.playbackOrder.indexOf(current.currentIndex)
                    current.copy(currentIndex = current.playbackOrder[position - 1])
                } else {
                    current
                }
            }
        }

        override fun moveTo(index: Int) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active || index !in current.entries.indices) {
                    return@update current
                }
                current.copy(currentIndex = index)
            }
        }

        override fun clear() {
            _state.value = PlaybackQueueState.Empty
        }

        override fun setShuffleEnabled(enabled: Boolean) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active || current.shuffleEnabled == enabled) {
                    return@update current
                }
                // Shuffle/sort the current queue members, not all folder entries: entries removed in this
                // session must not reappear when shuffle is toggled (ADR-008 non-goal).
                val newOrder =
                    if (enabled) {
                        shuffledOrderWithFirst(current.playbackOrder, current.currentIndex)
                    } else {
                        current.playbackOrder.sorted()
                    }
                current.copy(playbackOrder = newOrder, shuffleEnabled = enabled)
            }
        }

        override fun move(
            fromPosition: Int,
            toPosition: Int,
        ) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                if (fromPosition !in current.playbackOrder.indices || toPosition !in current.playbackOrder.indices) {
                    return@update current
                }
                if (fromPosition == toPosition) return@update current
                val newOrder = current.playbackOrder.toMutableList()
                newOrder.add(toPosition, newOrder.removeAt(fromPosition))
                current.copy(playbackOrder = newOrder)
            }
        }

        override fun deactivate(position: Int) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                if (position !in current.playbackOrder.indices) return@update current
                // Keep at least one active entry; the last active one cannot be deactivated.
                if (current.playbackOrder.size == 1) return@update current
                val newOrder = current.playbackOrder.toMutableList()
                val deactivatedEntryIndex = newOrder.removeAt(position)
                val newCurrentIndex =
                    if (deactivatedEntryIndex == current.currentIndex) {
                        // Advance to the successor that shifted into `position`, or fall back to the
                        // predecessor when the deactivated entry was last in the active order.
                        newOrder[position.coerceAtMost(newOrder.lastIndex)]
                    } else {
                        current.currentIndex
                    }
                current.copy(
                    playbackOrder = newOrder,
                    currentIndex = newCurrentIndex,
                    deactivated = current.deactivated + deactivatedEntryIndex,
                )
            }
        }

        override fun moveAfterCurrent(position: Int) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                if (position !in current.playbackOrder.indices) return@update current
                if (position == current.playbackOrder.indexOf(current.currentIndex)) return@update current
                val newOrder = current.playbackOrder.toMutableList()
                val moved = newOrder.removeAt(position)
                newOrder.add(newOrder.indexOf(current.currentIndex) + 1, moved)
                current.copy(playbackOrder = newOrder)
            }
        }

        override fun reactivate(trackIndex: Int) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                if (trackIndex !in current.deactivated) return@update current
                current.copy(
                    playbackOrder = current.playbackOrder + trackIndex,
                    deactivated = current.deactivated - trackIndex,
                )
            }
        }

        override fun reactivateAt(
            trackIndex: Int,
            position: Int,
        ) {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                if (trackIndex !in current.deactivated) return@update current
                val newOrder = current.playbackOrder.toMutableList()
                newOrder.add(position.coerceIn(0, newOrder.size), trackIndex)
                current.copy(
                    playbackOrder = newOrder,
                    deactivated = current.deactivated - trackIndex,
                )
            }
        }

        override fun reset() {
            _state.update { current ->
                if (current !is PlaybackQueueState.Active) return@update current
                current.copy(
                    playbackOrder = current.entries.indices.toList(),
                    deactivated = emptyList(),
                    shuffleEnabled = false,
                )
            }
        }

        private fun shuffledOrderWithFirst(
            order: List<Int>,
            firstIndex: Int,
        ): List<Int> {
            val rest = order.filter { it != firstIndex }.shuffled(random)
            return listOf(firstIndex) + rest
        }
    }
