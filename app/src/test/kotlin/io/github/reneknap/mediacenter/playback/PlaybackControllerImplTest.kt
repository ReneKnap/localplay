package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FakeAudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueImpl
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.playback.FakePlaybackPreferencesDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerImplTest {
    private lateinit var audioRepo: FakeAudioRepository
    private lateinit var queue: PlaybackQueueImpl
    private lateinit var engine: FakeMediaEngine
    private lateinit var playbackPrefs: FakePlaybackPreferencesDataSource

    @Before
    fun setUp() {
        audioRepo = FakeAudioRepository()
        queue = PlaybackQueueImpl(audioRepository = audioRepo, random = Random(42L))
        engine = FakeMediaEngine()
        playbackPrefs = FakePlaybackPreferencesDataSource()
    }

    private fun controller(scope: TestScope): PlaybackControllerImpl =
        PlaybackControllerImpl(
            queue = queue,
            engine = engine,
            playbackPreferences = playbackPrefs,
            scope = TestScope(UnconfinedTestDispatcher(scope.testScheduler)),
        )

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

    private fun ready(
        folderUri: String,
        tracks: List<AudioTrack>,
    ): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = FolderScanState.Ready(tracks),
        )

    // ---------------------------------------------------------------------
    // Push to engine — app action → engine
    // ---------------------------------------------------------------------

    @Test
    fun `prepareFolder pushes setQueue with sequential items, startIndex=0, playWhenReady=false`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)

            c.prepareFolder(folderUri)

            assertEquals(1, engine.setQueueHistory.size)
            val last = engine.setQueueHistory.last()
            assertEquals(tracks, last.items)
            assertEquals(0, last.startIndex)
            assertEquals(false, last.playWhenReady)
        }

    @Test
    fun `prepareFolder on empty folder leaves engine untouched`() =
        runTest {
            val c = controller(this)

            c.prepareFolder("content://music/unknown")

            assertEquals(0, engine.setQueueHistory.size)
        }

    @Test
    fun `prepareFolder on same folder is no-op for queue and engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next() // move queue forward via mirror
            val stateBefore = queue.state.value
            val setQueueCountBefore = engine.setQueueHistory.size

            c.prepareFolder(folderUri)

            assertEquals(stateBefore, queue.state.value)
            assertEquals(setQueueCountBefore, engine.setQueueHistory.size)
        }

    @Test
    fun `prepareFolder for different folder resets play and pushes new setQueue`() =
        runTest {
            val folderA = "content://music/a"
            val folderB = "content://music/b"
            val tracksA = listOf(track("$folderA/1"), track("$folderA/2"))
            val tracksB = listOf(track("$folderB/1"))
            audioRepo.emit(listOf(ready(folderA, tracksA), ready(folderB, tracksB)))
            val c = controller(this)
            c.prepareFolder(folderA)
            c.togglePlayPause() // engine.playWhenReady = true

            c.prepareFolder(folderB)

            val last = engine.setQueueHistory.last()
            assertEquals(tracksB, last.items)
            assertEquals(0, last.startIndex)
            assertEquals(false, last.playWhenReady)
            assertEquals(false, engine.playWhenReady.value)
        }

    @Test
    fun `prepareFolder with persisted shuffle pushes shuffled items`() =
        runTest {
            playbackPrefs = FakePlaybackPreferencesDataSource(initial = true)
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)

            c.prepareFolder(folderUri)
            testScheduler.advanceUntilIdle()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(true, state.shuffleEnabled)
            val last = engine.setQueueHistory.last()
            val expectedItems = state.playbackOrder.map { tracks[it] }
            assertEquals(expectedItems, last.items)
            assertEquals(tracks[0], last.items[0]) // shuffledOrderWithFirst anchors firstIndex=0 at position 0
        }

    @Test
    fun `playAtIndex with sequential order seeks engine to same index and sets playWhenReady=true`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.playAtIndex(2)

            assertEquals(FakeMediaEngine.Seek.MediaItem(2), engine.seekHistory.last())
            assertEquals(true, engine.playWhenReady.value)
        }

    @Test
    fun `playAtIndex with shuffled order seeks engine to playbackOrder position of that track`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.playAtIndex(2)
            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()
            val state = queue.state.value as PlaybackQueueState.Active
            val expectedPos = state.playbackOrder.indexOf(4)
            assertTrue("track index 4 must be in shuffled order", expectedPos >= 0)

            c.playAtIndex(4)

            assertEquals(FakeMediaEngine.Seek.MediaItem(expectedPos), engine.seekHistory.last())
            assertEquals(true, engine.playWhenReady.value)
        }

    @Test
    fun `playAtIndex with out-of-bounds index is no-op for engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            val seekCountBefore = engine.seekHistory.size

            c.playAtIndex(99)

            assertEquals(seekCountBefore, engine.seekHistory.size)
        }

    @Test
    fun `togglePlayPause from paused engine sets playWhenReady=true`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.togglePlayPause()

            assertEquals(true, engine.playWhenReady.value)
        }

    @Test
    fun `togglePlayPause from playing engine sets playWhenReady=false`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.togglePlayPause() // playing

            c.togglePlayPause()

            assertEquals(false, engine.playWhenReady.value)
        }

    @Test
    fun `next delegates to engine seekToNext`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.next()

            assertEquals(FakeMediaEngine.Seek.Next, engine.seekHistory.last())
        }

    @Test
    fun `previous delegates to engine seekToPrevious`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next()

            c.previous()

            assertEquals(FakeMediaEngine.Seek.Previous, engine.seekHistory.last())
        }

    // ---------------------------------------------------------------------
    // Mirror — engine → queue
    // ---------------------------------------------------------------------

    @Test
    fun `engine auto-advance mirrors to queue currentIndex when order is sequential`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            engine.simulateAutoAdvance()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
        }

    @Test
    fun `engine auto-advance mirrors to queue currentIndex via playbackOrder when shuffled`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.playAtIndex(2)
            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()
            val stateBefore = queue.state.value as PlaybackQueueState.Active
            val expectedTrackIndex = stateBefore.playbackOrder[1]

            engine.simulateAutoAdvance()

            val stateAfter = queue.state.value as PlaybackQueueState.Active
            assertEquals(expectedTrackIndex, stateAfter.currentIndex)
        }

    // ---------------------------------------------------------------------
    // Shuffle re-push
    // ---------------------------------------------------------------------

    @Test
    fun `setShuffleEnabled true on active queue re-pushes shuffled order with current track at startIndex 0`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.playAtIndex(2)
            val setQueueCountBefore = engine.setQueueHistory.size

            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()

            assertEquals(setQueueCountBefore + 1, engine.setQueueHistory.size)
            val last = engine.setQueueHistory.last()
            assertEquals(0, last.startIndex)
            assertEquals(tracks[2], last.items[0])
            assertEquals(5, last.items.size)
        }

    @Test
    fun `setShuffleEnabled false re-pushes sequential order with startIndex at current track natural position`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()
            engine.simulateAutoAdvance() // move off position 0 in shuffled order
            val currentTrackIndex = (queue.state.value as PlaybackQueueState.Active).currentIndex

            c.setShuffleEnabled(false)
            testScheduler.advanceUntilIdle()

            val last = engine.setQueueHistory.last()
            assertEquals(tracks, last.items)
            assertEquals(currentTrackIndex, last.startIndex)
        }

    @Test
    fun `setShuffleEnabled preserves playWhenReady across re-push`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.playAtIndex(2) // engine.playWhenReady = true

            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()

            assertEquals(true, engine.setQueueHistory.last().playWhenReady)
        }

    @Test
    fun `setShuffleEnabled persists value to preferences`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()

            assertEquals(true, playbackPrefs.shuffleEnabled.first())
        }

    @Test
    fun `setShuffleEnabled on inactive queue persists but does not push to engine`() =
        runTest {
            val c = controller(this)
            val setQueueCountBefore = engine.setQueueHistory.size

            c.setShuffleEnabled(true)
            testScheduler.advanceUntilIdle()

            assertEquals(setQueueCountBefore, engine.setQueueHistory.size)
            assertEquals(true, playbackPrefs.shuffleEnabled.first())
        }

    @Test
    fun `setShuffleEnabled with same value is no-op for engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            val setQueueCountBefore = engine.setQueueHistory.size

            c.setShuffleEnabled(false) // already false
            testScheduler.advanceUntilIdle()

            assertEquals(setQueueCountBefore, engine.setQueueHistory.size)
        }

    // ---------------------------------------------------------------------
    // Bootstrap / status flow
    // ---------------------------------------------------------------------

    @Test
    fun `bootstrap applies persisted shuffle to queue when preparing folder`() =
        runTest {
            playbackPrefs = FakePlaybackPreferencesDataSource(initial = true)
            val folderUri = "content://music/a"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)

            c.prepareFolder(folderUri)
            testScheduler.advanceUntilIdle()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(true, state.shuffleEnabled)
        }

    @Test
    fun `status flow reflects engine signals`() =
        runTest {
            val c = controller(this)
            engine.setDuration(60_000L)
            engine.setPlayWhenReady(true)
            engine.setPosition(12_345L)

            val s = c.status.value
            assertNotNull(s)
            assertTrue(s.isPlaying)
            assertEquals(60_000L, s.durationMs)
            assertEquals(12_345L, s.positionMs)
        }

    @Test
    fun `status flow exposes shuffleEnabled from preferences`() =
        runTest {
            playbackPrefs = FakePlaybackPreferencesDataSource(initial = true)
            val c = controller(this)
            testScheduler.advanceUntilIdle()

            assertEquals(true, c.status.value.shuffleEnabled)
        }
}
