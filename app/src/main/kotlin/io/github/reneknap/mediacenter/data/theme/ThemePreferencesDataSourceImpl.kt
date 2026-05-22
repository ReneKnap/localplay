package io.github.reneknap.mediacenter.data.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemePreferencesDataSourceImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ThemePreferencesDataSource {
        override val themeMode: Flow<ThemeMode> =
            dataStore.data.map { prefs ->
                val raw = prefs[THEME_MODE_KEY] ?: return@map ThemeMode.DARK
                runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.DARK)
            }

        override suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
        }

        private companion object {
            val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        }
    }
