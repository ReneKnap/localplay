package io.github.reneknap.mediacenter.data.theme

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeThemePreferencesDataSource(
    initial: ThemeMode = ThemeMode.DARK,
) : ThemePreferencesDataSource {
    private val state = MutableStateFlow(initial)

    override val themeMode: Flow<ThemeMode> = state.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
