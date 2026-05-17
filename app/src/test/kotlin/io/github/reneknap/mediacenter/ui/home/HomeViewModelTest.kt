package io.github.reneknap.mediacenter.ui.home

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.audio.FakeAudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var audioRepository: FakeAudioRepository
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        audioRepository = FakeAudioRepository()
        folderRepository = FakeFolderRepository()
        viewModel = HomeViewModel(audioRepository, folderRepository)
    }

    @Test
    fun `uiState reflects Empty when audio repository has no folders`() =
        runTest {
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
    fun `uiState reflects Folders when audio repository emits entries`() =
        runTest {
            val entry = FolderEntry("content://music/1", "Music", isReachable = true)
            val folderTracks = FolderTracks(entry, FolderScanState.Scanning)
            audioRepository.emit(listOf(folderTracks))

            viewModel.uiState.test {
                var state: HomeUiState = awaitItem()
                while (state !is HomeUiState.Folders) {
                    state = awaitItem()
                }
                assertEquals(listOf(folderTracks), state.items)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `addFolder delegates to folder repository`() =
        runTest {
            viewModel.addFolder("content://music/added")

            assertEquals(listOf("content://music/added"), folderRepository.addedFolders)
        }

    @Test
    fun `removeFolder delegates to folder repository`() =
        runTest {
            viewModel.removeFolder("content://music/removed")

            assertEquals(listOf("content://music/removed"), folderRepository.removedFolders)
        }
}
