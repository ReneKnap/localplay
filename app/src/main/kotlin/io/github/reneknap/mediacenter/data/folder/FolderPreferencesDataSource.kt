package io.github.reneknap.mediacenter.data.folder

import kotlinx.coroutines.flow.Flow

interface FolderPreferencesDataSource {
    val folders: Flow<List<FolderEntry>>
    suspend fun save(folders: List<FolderEntry>)
}
