package io.github.reneknap.mediacenter.playback

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FakeAudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueImpl
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueState
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerImplTest {
    private lateinit var audioRepo: FakeAudioRepository
    private lateinit var queue: PlaybackQueueImpl
    private lateinit var engine: FakeMediaEngine

    @Before
    fun setUp() {
        audioRepo = FakeAudioRepository()
        queue = PlaybackQueueImpl(audioRepository = audioRepo)
        engine = FakeMediaEngine()
    }

    private fun controller(scope: TestScope): PlaybackControllerImpl =
        PlaybackControllerImpl(
            queue = queue,
            engine = engine,
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

    @Test
    fun `prepareFolder loads first track and stays paused`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)

            c.prepareFolder(folderUri)

            assertSame(tracks[0], engine.loadedTrack)
            assertEquals(false, engine.lastPlayWhenReadyAtLoad)
            assertEquals(false, engine.isPlaying.value)
        }

    @Test
    fun `prepareFolder on empty folder leaves engine untouched`() =
        runTest {
            val c = controller(this)

            c.prepareFolder("content://music/unknown")

            assertNull(engine.loadedTrack)
        }

    @Test
    fun `playAtIndex moves queue and starts playback at that track`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.playAtIndex(2)

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(2, state.currentIndex)
            assertSame(tracks[2], engine.loadedTrack)
            assertEquals(true, engine.isPlaying.value)
        }

    @Test
    fun `playAtIndex with out-of-bounds index is no-op`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            val loadCountBefore = engine.loadHistory.size

            c.playAtIndex(99)

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
            assertEquals(loadCountBefore, engine.loadHistory.size)
            assertEquals(false, engine.isPlaying.value)
        }

    @Test
    fun `togglePlayPause from paused engine starts playback`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.togglePlayPause()

            assertEquals(true, engine.isPlaying.value)
        }

    @Test
    fun `togglePlayPause from playing engine pauses playback`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.togglePlayPause() // now playing

            c.togglePlayPause()

            assertEquals(false, engine.isPlaying.value)
        }

    @Test
    fun `next on non-last advances queue and engine reloads new track`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)

            c.next()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], engine.loadedTrack)
        }

    @Test
    fun `next on last track is no-op for queue and engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next() // index 1, last
            val loadCountBefore = engine.loadHistory.size

            c.next()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertEquals(loadCountBefore, engine.loadHistory.size)
        }

    @Test
    fun `previous on first track is no-op for queue and engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            val loadCountBefore = engine.loadHistory.size

            c.previous()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
            assertEquals(loadCountBefore, engine.loadHistory.size)
        }

    @Test
    fun `previous decrements queue and engine reloads`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next()
            c.next() // index 2

            c.previous()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], engine.loadedTrack)
        }

    @Test
    fun `track ended on non-last advances queue and keeps playing`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.togglePlayPause() // playing

            engine.triggerTrackEnded()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], engine.loadedTrack)
            assertEquals(true, engine.isPlaying.value)
        }

    @Test
    fun `track ended on last track stops engine`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next() // index 1, last
            c.togglePlayPause() // playing

            engine.triggerTrackEnded()

            val state = queue.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertEquals(false, engine.isPlaying.value)
        }

    @Test
    fun `next preserves play state across track switch`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.togglePlayPause() // playing

            c.next()

            assertEquals(true, engine.isPlaying.value)
            assertEquals(true, engine.lastPlayWhenReadyAtLoad)
        }

    @Test
    fun `prepareFolder is no-op when queue is already active for the same folder`() =
        runTest {
            val folderUri = "content://music/a"
            val tracks = listOf(track("$folderUri/1"), track("$folderUri/2"), track("$folderUri/3"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val c = controller(this)
            c.prepareFolder(folderUri)
            c.next()
            c.next() // currentIndex = 2
            val stateBefore = queue.state.value
            val loadCountBefore = engine.loadHistory.size

            c.prepareFolder(folderUri)

            assertEquals(stateBefore, queue.state.value)
            assertEquals(loadCountBefore, engine.loadHistory.size)
        }

    @Test
    fun `prepareFolder for new folder resets play intent and loads paused`() =
        runTest {
            val folderA = "content://music/a"
            val folderB = "content://music/b"
            val tracksA = listOf(track("$folderA/1"), track("$folderA/2"))
            val tracksB = listOf(track("$folderB/1"))
            audioRepo.emit(listOf(ready(folderA, tracksA), ready(folderB, tracksB)))
            val c = controller(this)
            c.prepareFolder(folderA)
            c.togglePlayPause() // playIntent=true, engine playing folder A

            c.prepareFolder(folderB)

            assertSame(tracksB[0], engine.loadedTrack)
            assertEquals(false, engine.lastPlayWhenReadyAtLoad)
            assertEquals(false, engine.isPlaying.value)
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
}
