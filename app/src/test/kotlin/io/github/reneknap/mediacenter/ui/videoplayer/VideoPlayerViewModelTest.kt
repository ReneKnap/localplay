package io.github.reneknap.mediacenter.ui.videoplayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.video.FakeVideoRepository
import io.github.reneknap.mediacenter.data.video.FolderVideos
import io.github.reneknap.mediacenter.data.video.VideoItem
import io.github.reneknap.mediacenter.data.video.VideoScanState
import io.github.reneknap.mediacenter.playback.FakeVideoPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var videoRepository: FakeVideoRepository
    private lateinit var videoPlayer: FakeVideoPlayer

    @Before
    fun setUp() {
        videoRepository = FakeVideoRepository()
        videoPlayer = FakeVideoPlayer()
    }

    private fun viewModel(
        folderUri: String,
        startVideoUri: String? = null,
    ): VideoPlayerViewModel {
        val args = mutableMapOf<String, Any?>("folderUri" to folderUri)
        if (startVideoUri != null) args["startVideoUri"] = startVideoUri
        return VideoPlayerViewModel(
            videoRepository = videoRepository,
            videoPlayer = videoPlayer,
            savedStateHandle = SavedStateHandle(args),
        )
    }

    private fun video(uri: String): VideoItem =
        VideoItem(
            uri = uri,
            folderUri = uri.substringBeforeLast('/'),
            displayName = uri.substringAfterLast('/'),
            mimeType = "video/mp4",
            sizeBytes = 0L,
            durationMs = 0L,
            width = 0,
            height = 0,
        )

    private fun readyFolder(
        folderUri: String,
        displayName: String,
        videos: List<VideoItem>,
    ): FolderVideos =
        FolderVideos(
            folder = FolderEntry(folderUri, displayName, isReachable = true),
            scan = VideoScanState.Ready(videos),
        )

    private fun scanningFolder(folderUri: String): FolderVideos =
        FolderVideos(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = VideoScanState.Scanning,
        )

    private fun unreachableFolder(folderUri: String): FolderVideos =
        FolderVideos(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = false),
            scan = VideoScanState.Unreachable,
        )

    @Test
    fun `scanning folder yields Loading`() =
        runTest {
            val folderUri = "content://video/scanning"
            videoRepository.emit(listOf(scanningFolder(folderUri)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                assertEquals(VideoPlayerUiState.Loading, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unreachable folder yields NotAvailable`() =
        runTest {
            val folderUri = "content://video/gone"
            videoRepository.emit(listOf(unreachableFolder(folderUri)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                var state: VideoPlayerUiState = awaitItem()
                while (state is VideoPlayerUiState.Loading) {
                    state = awaitItem()
                }
                assertEquals(VideoPlayerUiState.NotAvailable, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unknown folder uri yields NotAvailable`() =
        runTest {
            videoRepository.emit(emptyList())
            val vm = viewModel("content://video/unknown")

            vm.uiState.test {
                var state: VideoPlayerUiState = awaitItem()
                while (state is VideoPlayerUiState.Loading) {
                    state = awaitItem()
                }
                assertEquals(VideoPlayerUiState.NotAvailable, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ready folder with no videos yields EmptyFolder`() =
        runTest {
            val folderUri = "content://video/empty"
            videoRepository.emit(listOf(readyFolder(folderUri, "Empty", emptyList())))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                var state: VideoPlayerUiState = awaitItem()
                while (state is VideoPlayerUiState.Loading) {
                    state = awaitItem()
                }
                assertEquals(VideoPlayerUiState.EmptyFolder("Empty"), state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ready folder yields Ready with folder name and videos`() =
        runTest {
            val folderUri = "content://video/a"
            val videos = listOf(video("$folderUri/1.mp4"), video("$folderUri/2.mp4"))
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                var state: VideoPlayerUiState = awaitItem()
                while (state !is VideoPlayerUiState.Ready) {
                    state = awaitItem()
                }
                assertEquals("Clips", state.folderName)
                assertEquals(videos, state.videos)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ready folder feeds the player once with default start index`() =
        runTest {
            val folderUri = "content://video/a"
            val videos = listOf(video("$folderUri/1.mp4"), video("$folderUri/2.mp4"))
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))

            viewModel(folderUri)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(videos to 0), videoPlayer.setPlaylistCalls)
        }

    @Test
    fun `startVideoUri selects matching start index`() =
        runTest {
            val folderUri = "content://video/a"
            val videos = listOf(video("$folderUri/1.mp4"), video("$folderUri/2.mp4"), video("$folderUri/3.mp4"))
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))

            viewModel(folderUri, startVideoUri = "$folderUri/3.mp4")
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(videos to 2), videoPlayer.setPlaylistCalls)
        }

    @Test
    fun `startVideoUri with no match falls back to index zero`() =
        runTest {
            val folderUri = "content://video/a"
            val videos = listOf(video("$folderUri/1.mp4"), video("$folderUri/2.mp4"))
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))

            viewModel(folderUri, startVideoUri = "$folderUri/ghost.mp4")
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(videos to 0), videoPlayer.setPlaylistCalls)
        }

    @Test
    fun `re-emitting the same ready list feeds the player only once`() =
        runTest {
            val folderUri = "content://video/a"
            val videos = listOf(video("$folderUri/1.mp4"))
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))

            viewModel(folderUri)
            testScheduler.advanceUntilIdle()
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", videos)))
            testScheduler.advanceUntilIdle()

            assertEquals(1, videoPlayer.setPlaylistCalls.size)
        }

    @Test
    fun `empty ready folder does not feed the player`() =
        runTest {
            val folderUri = "content://video/empty"
            videoRepository.emit(listOf(readyFolder(folderUri, "Empty", emptyList())))

            viewModel(folderUri)
            testScheduler.advanceUntilIdle()

            assertEquals(emptyList<Pair<List<VideoItem>, Int>>(), videoPlayer.setPlaylistCalls)
        }

    @Test
    fun `pause delegates to the player`() =
        runTest {
            val folderUri = "content://video/a"
            videoRepository.emit(listOf(readyFolder(folderUri, "Clips", listOf(video("$folderUri/1.mp4")))))
            val vm = viewModel(folderUri)

            vm.pause()

            assertEquals(1, videoPlayer.pauseCount)
        }
}
