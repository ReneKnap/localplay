package io.github.reneknap.mediacenter.data.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaybackPreferencesDataSourceImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : PlaybackPreferencesDataSource {
        override val shuffleEnabled: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[SHUFFLE_ENABLED_KEY] ?: false }

        override suspend fun setShuffleEnabled(enabled: Boolean) {
            dataStore.edit { prefs -> prefs[SHUFFLE_ENABLED_KEY] = enabled }
        }

        private companion object {
            val SHUFFLE_ENABLED_KEY = booleanPreferencesKey("shuffle_enabled")
        }
    }
