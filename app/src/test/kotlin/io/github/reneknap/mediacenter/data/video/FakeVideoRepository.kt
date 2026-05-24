package io.github.reneknap.mediacenter.data.video

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeVideoRepository(
    initial: List<FolderVideos> = emptyList(),
) : VideoRepository {
    private val state = MutableStateFlow(initial)

    override val folders: Flow<List<FolderVideos>> = state.asStateFlow()

    fun emit(folders: List<FolderVideos>) {
        state.value = folders
    }
}
