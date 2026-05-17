package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioRepository(
    initial: List<FolderTracks> = emptyList(),
) : AudioRepository {
    private val state = MutableStateFlow(initial)

    override val folders: Flow<List<FolderTracks>> = state.asStateFlow()

    fun emit(folders: List<FolderTracks>) {
        state.value = folders
    }
}
