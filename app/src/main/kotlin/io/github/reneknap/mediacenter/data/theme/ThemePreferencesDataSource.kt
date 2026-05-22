package io.github.reneknap.mediacenter.data.theme

import kotlinx.coroutines.flow.Flow

interface ThemePreferencesDataSource {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
