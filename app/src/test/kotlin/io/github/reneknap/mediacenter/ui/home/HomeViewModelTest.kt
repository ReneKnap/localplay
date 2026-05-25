package io.github.reneknap.mediacenter.ui.home

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.media.FakeMediaRepository
import io.github.reneknap.mediacenter.data.media.FolderMediaContent
import io.github.reneknap.mediacenter.data.media.MediaContentScanState
import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.data.video.VideoItem
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

    private lateinit var mediaRepository: FakeMediaRepository
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var viewModel: HomeViewModel

    private val entry = FolderEntry("content://media/1", "Media", isReachable = true)

    @Before
    fun setUp() {
        mediaRepository = FakeMediaRepository()
        folderRepository = FakeFolderRepository()
        viewModel = HomeViewModel(mediaRepository, folderRepository)
    }

    private fun audio(uri: String): MediaEntry =
        MediaEntry.Audio(
            AudioTrack(
                uri = uri,
                folderUri = uri.substringBeforeLast('/'),
                displayName = uri.substringAfterLast('/'),
                mimeType = "audio/mpeg",
                sizeBytes = 0L,
                title = "Song",
                artist = null,
                album = null,
                durationMs = 0L,
            ),
        )

    private fun video(uri: String): MediaEntry =
        MediaEntry.Video(
            VideoItem(
                uri = uri,
                folderUri = uri.substringBeforeLast('/'),
                displayName = uri.substringAfterLast('/'),
                mimeType = "video/mp4",
                sizeBytes = 0L,
                durationMs = 1_000L,
                width = 1920,
                height = 1080,
            ),
        )

    @Test
    fun `uiState reflects Empty when repository has no folders`() =
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
    fun `uiState reflects Folders when repository emits entries`() =
        runTest {
            mediaRepository.emit(listOf(FolderMediaContent(entry, MediaContentScanState.Scanning)))

            viewModel.uiState.test {
                var state: HomeUiState = awaitItem()
                while (state !is HomeUiState.Folders) {
                    state = awaitItem()
                }
                assertEquals(1, state.items.size)
                assertEquals(entry, state.items[0].folder)
                assertEquals(MediaContentScanState.Scanning, state.items[0].content)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState exposes one combined entry list per folder`() =
        runTest {
            val entries = listOf(audio("${entry.uri}/song.mp3"), video("${entry.uri}/clip.mp4"))
            mediaRepository.emit(listOf(FolderMediaContent(entry, MediaContentScanState.Ready(entries))))

            viewModel.uiState.test {
                var state: HomeUiState = awaitItem()
                while (state !is HomeUiState.Folders || state.items[0].content !is MediaContentScanState.Ready) {
                    state = awaitItem()
                }
                val content = state.items[0].content as MediaContentScanState.Ready
                assertEquals(listOf("song.mp3", "clip.mp4"), content.entries.map { it.displayName })
                assertTrue(content.entries[0] is MediaEntry.Audio)
                assertTrue(content.entries[1] is MediaEntry.Video)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `addFolder delegates to folder repository`() =
        runTest {
            viewModel.addFolder("content://media/added")

            assertEquals(listOf("content://media/added"), folderRepository.addedFolders)
        }

    @Test
    fun `removeFolder delegates to folder repository`() =
        runTest {
            viewModel.removeFolder("content://media/removed")

            assertEquals(listOf("content://media/removed"), folderRepository.removedFolders)
        }
}
