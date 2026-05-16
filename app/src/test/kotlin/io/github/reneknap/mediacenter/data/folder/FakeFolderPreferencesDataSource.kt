package io.github.reneknap.mediacenter.data.folder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFolderPreferencesDataSource(
    initial: List<FolderEntry> = emptyList(),
) : FolderPreferencesDataSource {

    private val state = MutableStateFlow(initial)

    override val folders: Flow<List<FolderEntry>> = state.asStateFlow()

    override suspend fun save(folders: List<FolderEntry>) {
        state.value = folders
    }
}
