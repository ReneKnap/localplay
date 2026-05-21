package io.github.reneknap.mediacenter.ui.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.reneknap.mediacenter.MainDispatcherRule
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FakeArtworkReader
import io.github.reneknap.mediacenter.data.audio.FakeAudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueImpl
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.playback.FakePlaybackController
import io.github.reneknap.mediacenter.playback.PlayerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderPlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var audioRepository: FakeAudioRepository
    private lateinit var queue: PlaybackQueueImpl
    private lateinit var controller: FakePlaybackController
    private lateinit var artworkReader: FakeArtworkReader

    @Before
    fun setUp() {
        audioRepository = FakeAudioRepository()
        queue = PlaybackQueueImpl(audioRepository = audioRepository)
        controller = FakePlaybackController()
        artworkReader = FakeArtworkReader()
    }

    private fun viewModel(
        folderUri: String,
        startTrackUri: String? = null,
    ): FolderPlayerViewModel {
        val args = mutableMapOf<String, Any?>("folderUri" to folderUri)
        if (startTrackUri != null) args["startTrackUri"] = startTrackUri
        val handle = SavedStateHandle(args)
        return FolderPlayerViewModel(
            audioRepository = audioRepository,
            queue = queue,
            controller = controller,
            artworkReader = artworkReader,
            savedStateHandle = handle,
        )
    }

    private fun track(
        uri: String,
        title: String = uri.substringAfterLast('/'),
    ): AudioTrack =
        AudioTrack(
            uri = uri,
            folderUri = uri.substringBeforeLast('/'),
            displayName = "$title.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 0L,
            title = title,
            artist = null,
            album = null,
            durationMs = 0L,
        )

    private fun readyFolder(
        folderUri: String,
        displayName: String,
        tracks: List<AudioTrack>,
    ): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, displayName, isReachable = true),
            scan = FolderScanState.Ready(tracks),
        )

    private fun scanningFolder(folderUri: String): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = FolderScanState.Scanning,
        )

    private fun unreachableFolder(folderUri: String): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = false),
            scan = FolderScanState.Unreachable,
        )

    @Test
    fun `init invokes prepareFolder on controller`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))

            viewModel(folderUri)
            // Allow init coroutine to run
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(folderUri), controller.preparedFolders)
        }

    @Test
    fun `unknown folder uri yields NotAvailable`() =
        runTest {
            audioRepository.emit(emptyList())
            val vm = viewModel("content://music/unknown")

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state is FolderPlayerUiState.Loading) {
                    state = awaitItem()
                }
                assertEquals(FolderPlayerUiState.NotAvailable, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `scanning folder yields Loading`() =
        runTest {
            val folderUri = "content://music/scanning"
            audioRepository.emit(listOf(scanningFolder(folderUri)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                assertEquals(FolderPlayerUiState.Loading, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unreachable folder yields NotAvailable`() =
        runTest {
            val folderUri = "content://music/gone"
            audioRepository.emit(listOf(unreachableFolder(folderUri)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state is FolderPlayerUiState.Loading) {
                    state = awaitItem()
                }
                assertEquals(FolderPlayerUiState.NotAvailable, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ready folder yields Ready with folder name and tracks`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "Album A", tracks)))
            val vm = viewModel(folderUri)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready) {
                    state = awaitItem()
                }
                assertEquals("Album A", state.folderName)
                assertEquals(tracks, state.tracks)
                assertNull(state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selectTrack sets selectedIndex`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.selectTrack(1)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || state.selectedIndex != 1) {
                    state = awaitItem()
                }
                assertEquals(1, state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selectTrack on already-selected index clears selection`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.selectTrack(1)
            vm.selectTrack(1)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || state.selectedIndex != null) {
                    state = awaitItem()
                }
                assertNull(state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `play with no selection delegates playAtIndex 0`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.play()

            assertEquals(listOf(0), controller.playedIndexes)
        }

    @Test
    fun `play with selection delegates playAtIndex with selected index`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.selectTrack(2)
            vm.play()

            assertEquals(listOf(2), controller.playedIndexes)
        }

    @Test
    fun `togglePlayPause forwards to controller`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))
            val vm = viewModel(folderUri)

            vm.togglePlayPause()

            assertEquals(1, controller.togglePlayPauseCount)
        }

    @Test
    fun `next forwards to controller`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))
            val vm = viewModel(folderUri)

            vm.next()

            assertEquals(1, controller.nextCount)
        }

    @Test
    fun `previous forwards to controller`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))
            val vm = viewModel(folderUri)

            vm.previous()

            assertEquals(1, controller.previousCount)
        }

    @Test
    fun `selectTrack during playback switches to that track via controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri)
            controller.emitStatus(PlayerStatus(isPlaying = true))
            val vm = viewModel(folderUri)

            vm.selectTrack(2)

            assertEquals(listOf(2), controller.playedIndexes)
        }

    @Test
    fun `selectTrack on currently playing track is no-op`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri) // currentIndex = 0
            controller.emitStatus(PlayerStatus(isPlaying = true))
            val vm = viewModel(folderUri)

            vm.selectTrack(0)

            assertEquals(emptyList<Int>(), controller.playedIndexes)
        }

    @Test
    fun `startTrackUri sets initial selection to that track`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri, startTrackUri = "$folderUri/3")

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || state.selectedIndex != 2) {
                    state = awaitItem()
                }
                assertEquals(2, state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startTrackUri not matching any track leaves selection null`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri, startTrackUri = "$folderUri/ghost")

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready) {
                    state = awaitItem()
                }
                assertNull(state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleShuffle delegates to controller with negated current value`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))
            controller.emitStatus(PlayerStatus(shuffleEnabled = false))
            val vm = viewModel(folderUri)
            testScheduler.advanceUntilIdle()

            vm.toggleShuffle()

            assertEquals(listOf(true), controller.shuffleEnabledCalls)
        }

    @Test
    fun `toggleShuffle from enabled state delegates false to controller`() =
        runTest {
            val folderUri = "content://music/a"
            audioRepository.emit(listOf(readyFolder(folderUri, "A", listOf(track("$folderUri/1")))))
            controller.emitStatus(PlayerStatus(shuffleEnabled = true))
            val vm = viewModel(folderUri)
            testScheduler.advanceUntilIdle()

            vm.toggleShuffle()

            assertEquals(listOf(false), controller.shuffleEnabledCalls)
        }

    @Test
    fun `toggleShuffle anchors selected track via queue moveTo before enabling`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri)
            controller.emitStatus(PlayerStatus(shuffleEnabled = false))
            val vm = viewModel(folderUri)
            testScheduler.advanceUntilIdle()
            vm.selectTrack(3)
            testScheduler.advanceUntilIdle()

            vm.toggleShuffle()

            val state =
                queue.state.value as io.github.reneknap.mediacenter.data.audio.PlaybackQueueState.Active
            assertEquals(3, state.currentIndex)
            assertEquals(listOf(true), controller.shuffleEnabledCalls)
        }

    @Test
    fun `Ready state surfaces shuffleEnabled from controller status`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)
            controller.emitStatus(PlayerStatus(shuffleEnabled = true))

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || !state.shuffleEnabled) {
                    state = awaitItem()
                }
                assertTrue(state.shuffleEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Ready state status reflects controller status flow`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)
            controller.emitStatus(PlayerStatus(isPlaying = true, positionMs = 5_000L, durationMs = 60_000L))

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || !state.status.isPlaying) {
                    state = awaitItem()
                }
                assertTrue(state.status.isPlaying)
                assertEquals(5_000L, state.status.positionMs)
                assertEquals(60_000L, state.status.durationMs)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---------------------------------------------------------------------
    // Editable queue intents
    // ---------------------------------------------------------------------

    @Test
    fun `moveTrack delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.moveTrack(0, 2)

            assertEquals(listOf(0 to 2), controller.movedTracks)
        }

    @Test
    fun `deactivateTrack delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri)
            val vm = viewModel(folderUri)

            vm.deactivateTrack(1)

            assertEquals(listOf(1), controller.deactivatedPositions)
        }

    @Test
    fun `playTrackNext delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.playTrackNext(2)

            assertEquals(listOf(2), controller.playNextPositions)
        }

    @Test
    fun `reactivateTrack delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.reactivateTrack(2)

            assertEquals(listOf(2), controller.reactivatedTracks)
        }

    @Test
    fun `reactivateTrackAt delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.reactivateTrackAt(2, 1)

            assertEquals(listOf(2 to 1), controller.reactivatedAtTracks)
        }

    @Test
    fun `resetQueue delegates to controller`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            val vm = viewModel(folderUri)

            vm.resetQueue()

            assertEquals(1, controller.resetQueueCount)
        }

    @Test
    fun `deactivateTrack clears selectedIndex when the selected track is deactivated`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri)
            val vm = viewModel(folderUri)
            vm.selectTrack(1)

            vm.deactivateTrack(1)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready) {
                    state = awaitItem()
                }
                assertNull(state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deactivateTrack keeps selectedIndex when a different track is deactivated`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (0..2).map { track("$folderUri/$it") }
            audioRepository.emit(listOf(readyFolder(folderUri, "A", tracks)))
            queue.setQueue(folderUri)
            val vm = viewModel(folderUri)
            vm.selectTrack(2)

            vm.deactivateTrack(0)

            vm.uiState.test {
                var state: FolderPlayerUiState = awaitItem()
                while (state !is FolderPlayerUiState.Ready || state.selectedIndex != 2) {
                    state = awaitItem()
                }
                assertEquals(2, state.selectedIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
