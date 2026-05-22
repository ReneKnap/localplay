package io.github.reneknap.mediacenter.ui.theme

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.theme.FakeThemePreferencesDataSource
import io.github.reneknap.mediacenter.data.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `themeMode reflects the persisted mode`() =
        runTest {
            val viewModel = ThemeViewModel(FakeThemePreferencesDataSource(initial = ThemeMode.LIGHT))

            viewModel.themeMode.test {
                var mode = awaitItem()
                while (mode != ThemeMode.LIGHT) {
                    mode = awaitItem()
                }
                assertEquals(ThemeMode.LIGHT, mode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cycleTheme advances the persisted mode to the next`() =
        runTest {
            val viewModel = ThemeViewModel(FakeThemePreferencesDataSource(initial = ThemeMode.DARK))

            viewModel.themeMode.test {
                var mode = awaitItem()
                while (mode != ThemeMode.DARK) {
                    mode = awaitItem()
                }

                viewModel.cycleTheme()

                while (mode != ThemeMode.LIGHT) {
                    mode = awaitItem()
                }
                assertEquals(ThemeMode.LIGHT, mode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cycleTheme from SYSTEM wraps to DARK`() =
        runTest {
            val viewModel = ThemeViewModel(FakeThemePreferencesDataSource(initial = ThemeMode.SYSTEM))

            viewModel.themeMode.test {
                var mode = awaitItem()
                while (mode != ThemeMode.SYSTEM) {
                    mode = awaitItem()
                }

                viewModel.cycleTheme()

                while (mode != ThemeMode.DARK) {
                    mode = awaitItem()
                }
                assertEquals(ThemeMode.DARK, mode)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
