package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.folder.FolderEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackQueueImplTest {
    private lateinit var audioRepo: FakeAudioRepository

    @Before
    fun setUp() {
        audioRepo = FakeAudioRepository()
    }

    private fun queue(random: Random = Random.Default): PlaybackQueueImpl =
        PlaybackQueueImpl(audioRepository = audioRepo, random = random)

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

    private fun scanning(folderUri: String): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = FolderScanState.Scanning,
        )

    private fun unreachable(folderUri: String): FolderTracks =
        FolderTracks(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = false),
            scan = FolderScanState.Unreachable,
        )

    @Test
    fun `initial state is Empty`() {
        val q = queue()
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    @Test
    fun `setQueue on Ready folder yields Active with currentIndex zero`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)

            val state = q.state.value
            assertTrue(state is PlaybackQueueState.Active)
            val active = state as PlaybackQueueState.Active
            assertEquals(tracks, active.tracks)
            assertEquals(0, active.currentIndex)
            assertSame(tracks[0], active.current)
            assertTrue(active.hasNext)
            assertTrue(!active.hasPrevious)
        }

    @Test
    fun `setQueue on Scanning folder yields Empty`() =
        runTest {
            val folderUri = "content://music/still-scanning"
            audioRepo.emit(listOf(scanning(folderUri)))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on Unreachable folder yields Empty`() =
        runTest {
            val folderUri = "content://music/gone"
            audioRepo.emit(listOf(unreachable(folderUri)))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on Ready folder with empty tracks yields Empty`() =
        runTest {
            val folderUri = "content://music/empty"
            audioRepo.emit(listOf(ready(folderUri, emptyList())))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on unknown folder uri yields Empty`() =
        runTest {
            audioRepo.emit(listOf(ready("content://music/other", listOf(track("content://music/other/x")))))

            val q = queue()
            q.setQueue("content://music/does-not-exist")

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `moveToNext on Active with hasNext advances currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveToNext()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], state.current)
            assertTrue(state.hasPrevious)
            assertTrue(state.hasNext)
        }

    @Test
    fun `moveToNext on Active at last index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveToNext() // index 1, last
            val before = q.state.value
            q.moveToNext() // no-op

            assertEquals(before, q.state.value)
            assertEquals(1, (q.state.value as PlaybackQueueState.Active).currentIndex)
        }

    @Test
    fun `moveToPrevious on Active with hasPrevious decreases currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveToNext()
            q.moveToNext() // index 2
            q.moveToPrevious() // back to 1

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], state.current)
        }

    @Test
    fun `moveToPrevious on Active at first index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value
            q.moveToPrevious() // no-op

            assertEquals(before, q.state.value)
            assertEquals(0, (q.state.value as PlaybackQueueState.Active).currentIndex)
        }

    @Test
    fun `move methods on Empty are no-op`() {
        val q = queue()
        q.moveToNext()
        q.moveToPrevious()
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    @Test
    fun `clear from Active returns to Empty`() =
        runTest {
            val folderUri = "content://music/album"
            audioRepo.emit(listOf(ready(folderUri, listOf(track("$folderUri/a")))))

            val q = queue()
            q.setQueue(folderUri)
            q.clear()

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `snapshot — repository change to same folder does not mutate active queue`() =
        runTest {
            val folderUri = "content://music/album"
            val initialTracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, initialTracks)))

            val q = queue()
            q.setQueue(folderUri)

            // Repository now emits a *different* track list for the same folder.
            val newTracks = listOf(track("$folderUri/x"), track("$folderUri/y"), track("$folderUri/z"))
            audioRepo.emit(listOf(ready(folderUri, newTracks)))

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(initialTracks, state.tracks)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `snapshot — repository eviction of source folder does not mutate active queue`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)

            // Folder is removed from the repository entirely.
            audioRepo.emit(emptyList())

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(tracks, state.tracks)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue replaces queue wholesale across folders`() =
        runTest {
            val uriA = "content://music/a"
            val uriB = "content://music/b"
            val tracksA = listOf(track("$uriA/1"), track("$uriA/2"))
            val tracksB = listOf(track("$uriB/1"))
            audioRepo.emit(listOf(ready(uriA, tracksA), ready(uriB, tracksB)))

            val q = queue()
            q.setQueue(uriA)
            q.moveToNext() // currentIndex = 1 in A
            q.setQueue(uriB)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(tracksB, state.tracks)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri matching track sets currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/b")

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(tracks[1], state.current)
        }

    @Test
    fun `setQueue with startTrackUri not matching any track falls back to index 0`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/does-not-exist")

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri null uses index 0`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = null)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri on unreachable folder yields Empty`() =
        runTest {
            val folderUri = "content://music/gone"
            audioRepo.emit(listOf(unreachable(folderUri)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/anything")

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `moveTo with valid index updates currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(2)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(2, state.currentIndex)
            assertSame(tracks[2], state.current)
        }

    @Test
    fun `moveTo with negative index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value
            q.moveTo(-1)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `moveTo with index equal to size is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value
            q.moveTo(tracks.size)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `moveTo on Empty queue is no-op`() {
        val q = queue()
        q.moveTo(0)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    @Test
    fun `setQueue creates Active with sequential playback order and shuffle disabled`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = listOf(track("$folderUri/a"), track("$folderUri/b"), track("$folderUri/c"))
            audioRepo.emit(listOf(ready(folderUri, tracks)))

            val q = queue()
            q.setQueue(folderUri)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 2), state.playbackOrder)
            assertEquals(false, state.shuffleEnabled)
        }

    @Test
    fun `setShuffleEnabled true anchors current track at order position 0 and permutes the rest`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.moveTo(2)

            q.setShuffleEnabled(enabled = true)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(2, state.currentIndex)
            assertEquals(2, state.playbackOrder[0])
            assertEquals(setOf(0, 1, 3, 4), state.playbackOrder.drop(1).toSet())
            assertEquals(true, state.shuffleEnabled)
        }

    @Test
    fun `setShuffleEnabled false resets order to identity and keeps currentIndex on same track`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.moveTo(2)
            q.setShuffleEnabled(enabled = true)
            val currentTrackBefore = (q.state.value as PlaybackQueueState.Active).current

            q.setShuffleEnabled(enabled = false)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 2, 3, 4), state.playbackOrder)
            assertEquals(false, state.shuffleEnabled)
            assertSame(currentTrackBefore, state.current)
        }

    @Test
    fun `moveToNext during shuffle traverses playback order not tracks order`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.setShuffleEnabled(enabled = true)
            val orderBefore = (q.state.value as PlaybackQueueState.Active).playbackOrder

            q.moveToNext()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(orderBefore[1], state.currentIndex)
        }

    @Test
    fun `moveToPrevious during shuffle traverses playback order back`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.setShuffleEnabled(enabled = true)
            q.moveToNext()
            q.moveToNext()
            val orderBefore = (q.state.value as PlaybackQueueState.Active).playbackOrder

            q.moveToPrevious()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(orderBefore[1], state.currentIndex)
        }

    @Test
    fun `moveTo during shuffle changes currentIndex but keeps playback order stable`() =
        runTest {
            val folderUri = "content://music/album"
            val tracks = (1..5).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.setShuffleEnabled(enabled = true)
            val orderBefore = (q.state.value as PlaybackQueueState.Active).playbackOrder

            q.moveTo(3)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(3, state.currentIndex)
            assertEquals(orderBefore, state.playbackOrder)
            assertEquals(true, state.shuffleEnabled)
        }

    @Test
    fun `setShuffleEnabled on Empty queue is no-op`() {
        val q = queue()
        q.setShuffleEnabled(enabled = true)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    @Test
    fun `shuffle produces a non-identity order for a sufficiently long queue`() =
        runTest {
            // Sanity check that the seeded shuffle actually moves things around — without this
            // the other shuffle tests could pass trivially against a no-op implementation.
            val folderUri = "content://music/album"
            val tracks = (1..8).map { track("$folderUri/$it") }
            audioRepo.emit(listOf(ready(folderUri, tracks)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)

            q.setShuffleEnabled(enabled = true)

            val state = q.state.value as PlaybackQueueState.Active
            assertNotEquals(tracks.indices.toList(), state.playbackOrder)
        }
}
