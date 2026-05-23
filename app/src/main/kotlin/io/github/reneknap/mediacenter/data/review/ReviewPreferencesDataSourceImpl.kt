package io.github.reneknap.mediacenter.data.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReviewPreferencesDataSourceImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ReviewPreferencesDataSource {
        override val appStartCount: Flow<Int> =
            dataStore.data.map { prefs -> prefs[APP_START_COUNT_KEY] ?: 0 }

        override val supportHintDismissed: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[SUPPORT_HINT_DISMISSED_KEY] ?: false }

        override suspend fun incrementAppStartCount() {
            dataStore.edit { prefs ->
                prefs[APP_START_COUNT_KEY] = (prefs[APP_START_COUNT_KEY] ?: 0) + 1
            }
        }

        override suspend fun markSupportHintDismissed() {
            dataStore.edit { prefs -> prefs[SUPPORT_HINT_DISMISSED_KEY] = true }
        }

        private companion object {
            val APP_START_COUNT_KEY = intPreferencesKey("app_start_count")
            val SUPPORT_HINT_DISMISSED_KEY = booleanPreferencesKey("support_hint_dismissed")
        }
    }
