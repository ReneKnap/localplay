package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMediaScanIndex(
    initial: List<FolderMedia> = emptyList(),
) : MediaScanIndex {
    private val state = MutableStateFlow(initial)

    override val folders: Flow<List<FolderMedia>> = state.asStateFlow()

    fun emit(folders: List<FolderMedia>) {
        state.value = folders
    }
}
