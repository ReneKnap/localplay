package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot: the active queue is frozen at [setQueue] time; later [AudioRepository.folders]
 * emissions for the same folder do not mutate it.
 */
@Singleton
class PlaybackQueueImpl
    @Inject
    constructor(
        private val audioRepository: AudioRepository,
    ) : PlaybackQueue {
        private val _state = MutableStateFlow<PlaybackQueueState>(PlaybackQueueState.Empty)
        override val state: StateFlow<PlaybackQueueState> = _state.asStateFlow()

        override suspend fun setQueue(
            folderUri: String,
            startTrackUri: String?,
        ) {
            val folders = audioRepository.folders.first()
            val scan = folders.firstOrNull { it.folder.uri == folderUri }?.scan
            if (scan !is FolderScanState.Ready || scan.tracks.isEmpty()) {
                _state.value = PlaybackQueueState.Empty
                return
            }
            val startIndex = scan.tracks.indexOfFirst { it.uri == startTrackUri }.takeIf { it >= 0 } ?: 0
            _state.value = PlaybackQueueState.Active(scan.tracks, currentIndex = startIndex)
        }

        override fun moveToNext() {
            _state.update { current ->
                if (current is PlaybackQueueState.Active && current.hasNext) {
                    current.copy(currentIndex = current.currentIndex + 1)
                } else {
                    current
                }
            }
        }

        override fun moveToPrevious() {
            _state.update { current ->
                if (current is PlaybackQueueState.Active && current.hasPrevious) {
                    current.copy(currentIndex = current.currentIndex - 1)
                } else {
                    current
                }
            }
        }

        override fun moveTo(index: Int) {
            _state.update { current ->
                if (current is PlaybackQueueState.Active && index in current.tracks.indices) {
                    current.copy(currentIndex = index)
                } else {
                    current
                }
            }
        }

        override fun clear() {
            _state.value = PlaybackQueueState.Empty
        }
    }
