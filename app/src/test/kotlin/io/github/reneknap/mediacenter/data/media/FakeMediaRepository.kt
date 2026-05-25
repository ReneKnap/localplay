package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMediaRepository(
    initial: List<FolderMediaContent> = emptyList(),
) : MediaRepository {
    private val state = MutableStateFlow(initial)

    override val folders: Flow<List<FolderMediaContent>> = state.asStateFlow()

    fun emit(folders: List<FolderMediaContent>) {
        state.value = folders
    }
}
