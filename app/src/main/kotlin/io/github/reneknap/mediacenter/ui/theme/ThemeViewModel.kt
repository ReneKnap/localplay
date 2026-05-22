package io.github.reneknap.mediacenter.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.theme.ThemeMode
import io.github.reneknap.mediacenter.data.theme.ThemePreferencesDataSource
import io.github.reneknap.mediacenter.data.theme.next
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val themePreferences: ThemePreferencesDataSource,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode> =
            themePreferences.themeMode
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ThemeMode.DARK,
                )

        fun cycleTheme() {
            viewModelScope.launch {
                val current = themePreferences.themeMode.first()
                themePreferences.setThemeMode(current.next())
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
