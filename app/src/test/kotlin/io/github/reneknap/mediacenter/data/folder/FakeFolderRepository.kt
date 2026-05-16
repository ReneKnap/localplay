package io.github.reneknap.mediacenter.data.folder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFolderRepository(
    initial: List<FolderEntry> = emptyList(),
) : FolderRepository {

    private val state = MutableStateFlow(initial)

    val addedFolders: MutableList<String> = mutableListOf()
    val removedFolders: MutableList<String> = mutableListOf()

    override val folders: Flow<List<FolderEntry>> = state.asStateFlow()

    fun emit(folders: List<FolderEntry>) {
        state.value = folders
    }

    override suspend fun addFolder(uri: String) {
        addedFolders.add(uri)
    }

    override suspend fun removeFolder(uri: String) {
        removedFolders.add(uri)
    }
}
