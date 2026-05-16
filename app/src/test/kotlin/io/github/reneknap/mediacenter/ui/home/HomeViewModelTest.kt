package io.github.reneknap.mediacenter.ui.home

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeFolderRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        repository = FakeFolderRepository()
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `uiState reflects Empty when repository has no folders`() = runTest {
        viewModel.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assertEquals(HomeUiState.Empty, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects Folders when repository emits entries`() = runTest {
        val entry = FolderEntry("content://music/1", "Music", isReachable = true)
        repository.emit(listOf(entry))

        viewModel.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state !is HomeUiState.Folders) {
                state = awaitItem()
            }
            assertTrue(state is HomeUiState.Folders)
            assertEquals(listOf(entry), state.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addFolder delegates to repository`() = runTest {
        viewModel.addFolder("content://music/added")

        assertEquals(listOf("content://music/added"), repository.addedFolders)
    }

    @Test
    fun `removeFolder delegates to repository`() = runTest {
        viewModel.removeFolder("content://music/removed")

        assertEquals(listOf("content://music/removed"), repository.removedFolders)
    }
}
