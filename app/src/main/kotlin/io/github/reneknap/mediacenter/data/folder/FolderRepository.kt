package io.github.reneknap.mediacenter.data.folder

import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    val folders: Flow<List<FolderEntry>>
    suspend fun addFolder(uri: String)
    suspend fun removeFolder(uri: String)
}
