package io.github.reneknap.mediacenter.ui.home

import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FakeAudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.video.FakeVideoRepository
import io.github.reneknap.mediacenter.data.video.FolderVideos
import io.github.reneknap.mediacenter.data.video.VideoItem
import io.github.reneknap.mediacenter.data.video.VideoScanState
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

    private lateinit var audioRepository: FakeAudioRepository
    private lateinit var videoRepository: FakeVideoRepository
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var viewModel: HomeViewModel

    private val entry = FolderEntry("content://media/1", "Media", isReachable = true)

    @Before
    fun setUp() {
        audioRepository = FakeAudioRepository()
        videoRepository = FakeVideoRepository()
        folderRepository = FakeFolderRepository()
        viewModel = HomeViewModel(audioRepository, videoRepository, folderRepository)
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
            audioRepository.emit(listOf(FolderTracks(entry, FolderScanState.Scanning)))

            viewModel.uiState.test {
                var state: HomeUiState = awaitItem()
                while (state !is HomeUiState.Folders) {
                    state = awaitItem()
                }
                assertEquals(1, state.items.size)
                assertEquals(entry, state.items[0].folder)
                assertEquals(FolderScanState.Scanning, state.items[0].audio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState combines audio and video scan state per folder`() =
        runTest {
            val track =
                AudioTrack(
                    uri = "${entry.uri}/song.mp3",
                    folderUri = entry.uri,
                    displayName = "song.mp3",
                    mimeType = "audio/mpeg",
                    sizeBytes = 0L,
                    title = "Song",
                    artist = null,
                    album = null,
                    durationMs = 0L,
                )
            val video =
                VideoItem(
                    uri = "${entry.uri}/clip.mp4",
                    folderUri = entry.uri,
                    displayName = "clip.mp4",
                    mimeType = "video/mp4",
                    sizeBytes = 0L,
                    durationMs = 1_000L,
                    width = 1920,
                    height = 1080,
                )
            audioRepository.emit(listOf(FolderTracks(entry, FolderScanState.Ready(listOf(track)))))
            videoRepository.emit(listOf(FolderVideos(entry, VideoScanState.Ready(listOf(video)))))

            viewModel.uiState.test {
                var state: HomeUiState = awaitItem()
                while (state !is HomeUiState.Folders || state.items[0].video !is VideoScanState.Ready) {
                    state = awaitItem()
                }
                val item = state.items[0]
                assertTrue(item.audio is FolderScanState.Ready)
                assertTrue(item.video is VideoScanState.Ready)
                assertEquals(listOf("song.mp3"), (item.audio as FolderScanState.Ready).tracks.map { it.displayName })
                assertEquals(listOf("clip.mp4"), (item.video as VideoScanState.Ready).videos.map { it.displayName })
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
