package io.github.reneknap.mediacenter.data.folder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val dataSource: FolderPreferencesDataSource,
    private val access: FolderAccess,
) : FolderRepository {

    override val folders: Flow<List<FolderEntry>> = dataSource.folders
        .map { entries ->
            entries.map { it.copy(isReachable = access.canRead(it.uri)) }
        }
        .flowOn(Dispatchers.IO)

    override suspend fun addFolder(uri: String) {
        val current = dataSource.folders.first()
        if (current.any { it.uri == uri }) return
        access.takePersistable(uri)
        val displayName = access.displayName(uri) ?: uri
        dataSource.save(current + FolderEntry(uri, displayName))
    }

    override suspend fun removeFolder(uri: String) {
        access.releasePersistable(uri)
        val current = dataSource.folders.first()
        dataSource.save(current.filterNot { it.uri == uri })
    }
}
