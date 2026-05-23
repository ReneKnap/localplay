package io.github.reneknap.mediacenter.ui.home

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.review.FakeReviewPreferencesDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportHintViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `below the threshold the hint stays hidden and the counter increments`() =
        runTest {
            val preferences = FakeReviewPreferencesDataSource(initialCount = 2)

            val viewModel = SupportHintViewModel(preferences)

            assertEquals(3, preferences.appStartCount.first())
            viewModel.showSupportHint.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reaching the threshold on this start shows the hint`() =
        runTest {
            val preferences = FakeReviewPreferencesDataSource(initialCount = 7)

            val viewModel = SupportHintViewModel(preferences)

            assertEquals(8, preferences.appStartCount.first())
            viewModel.showSupportHint.test {
                var shown = awaitItem()
                while (!shown) {
                    shown = awaitItem()
                }
                assertTrue(shown)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a dismissed hint stays hidden and the counter does not increment`() =
        runTest {
            val preferences =
                FakeReviewPreferencesDataSource(initialCount = 8, initialDismissed = true)

            val viewModel = SupportHintViewModel(preferences)

            assertEquals(8, preferences.appStartCount.first())
            viewModel.showSupportHint.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `opening the support menu dismisses the hint permanently`() =
        runTest {
            val preferences = FakeReviewPreferencesDataSource(initialCount = 8)
            val viewModel = SupportHintViewModel(preferences)

            viewModel.onSupportMenuOpened()

            assertTrue(preferences.supportHintDismissed.first())
            viewModel.showSupportHint.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the counter does not increment past the threshold`() =
        runTest {
            val preferences = FakeReviewPreferencesDataSource(initialCount = 8)

            val viewModel = SupportHintViewModel(preferences)

            assertEquals(8, preferences.appStartCount.first())
            viewModel.showSupportHint.test {
                var shown = awaitItem()
                while (!shown) {
                    shown = awaitItem()
                }
                assertTrue(shown)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
