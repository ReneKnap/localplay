package io.github.reneknap.mediacenter.data.folder

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class FolderPreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : FolderPreferencesDataSource {

    override val folders: Flow<List<FolderEntry>> = dataStore.data
        .map { prefs ->
            val raw = prefs[FOLDERS_KEY] ?: return@map emptyList()
            try {
                json.decodeFromString<List<FolderEntry>>(raw)
            } catch (_: SerializationException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }

    override suspend fun save(folders: List<FolderEntry>) {
        dataStore.edit { prefs ->
            prefs[FOLDERS_KEY] = json.encodeToString(folders)
        }
    }

    private companion object {
        val FOLDERS_KEY = stringPreferencesKey("folders")
    }
}
