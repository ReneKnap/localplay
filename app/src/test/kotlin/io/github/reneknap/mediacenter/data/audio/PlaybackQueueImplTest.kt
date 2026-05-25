package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.media.FakeMediaRepository
import io.github.reneknap.mediacenter.data.media.FolderMediaContent
import io.github.reneknap.mediacenter.data.media.MediaContentScanState
import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.data.video.VideoItem
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
    private lateinit var mediaRepo: FakeMediaRepository

    @Before
    fun setUp() {
        mediaRepo = FakeMediaRepository()
    }

    private fun queue(random: Random = Random.Default): PlaybackQueueImpl =
        PlaybackQueueImpl(mediaRepository = mediaRepo, random = random)

    private fun audio(
        uri: String,
        title: String = uri.substringAfterLast('/'),
    ): MediaEntry =
        MediaEntry.Audio(
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
            ),
        )

    private fun video(
        uri: String,
        name: String = uri.substringAfterLast('/'),
    ): MediaEntry =
        MediaEntry.Video(
            VideoItem(
                uri = uri,
                folderUri = uri.substringBeforeLast('/'),
                displayName = "$name.mp4",
                mimeType = "video/mp4",
                sizeBytes = 0L,
                durationMs = 0L,
                width = 0,
                height = 0,
            ),
        )

    private fun ready(
        folderUri: String,
        entries: List<MediaEntry>,
    ): FolderMediaContent =
        FolderMediaContent(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = MediaContentScanState.Ready(entries),
        )

    private fun scanning(folderUri: String): FolderMediaContent =
        FolderMediaContent(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = true),
            scan = MediaContentScanState.Scanning,
        )

    private fun unreachable(folderUri: String): FolderMediaContent =
        FolderMediaContent(
            folder = FolderEntry(folderUri, folderUri.substringAfterLast('/'), isReachable = false),
            scan = MediaContentScanState.Unreachable,
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
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)

            val state = q.state.value
            assertTrue(state is PlaybackQueueState.Active)
            val active = state as PlaybackQueueState.Active
            assertEquals(entries, active.entries)
            assertEquals(0, active.currentIndex)
            assertSame(entries[0], active.current)
            assertTrue(active.hasNext)
            assertTrue(!active.hasPrevious)
        }

    @Test
    fun `setQueue on a mixed audio and video folder builds one interleaved active queue`() =
        runTest {
            val folderUri = "content://media/album"
            val entries =
                listOf(audio("$folderUri/a"), video("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)

            val active = q.state.value as PlaybackQueueState.Active
            assertEquals(entries, active.entries)
            assertEquals(listOf(0, 1, 2), active.playbackOrder)
            assertTrue(active.entries[1] is MediaEntry.Video)
        }

    @Test
    fun `setQueue on Scanning folder yields Empty`() =
        runTest {
            val folderUri = "content://music/still-scanning"
            mediaRepo.emit(listOf(scanning(folderUri)))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on Unreachable folder yields Empty`() =
        runTest {
            val folderUri = "content://music/gone"
            mediaRepo.emit(listOf(unreachable(folderUri)))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on Ready folder with empty entries yields Empty`() =
        runTest {
            val folderUri = "content://music/empty"
            mediaRepo.emit(listOf(ready(folderUri, emptyList())))

            val q = queue()
            q.setQueue(folderUri)

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `setQueue on unknown folder uri yields Empty`() =
        runTest {
            mediaRepo.emit(listOf(ready("content://music/other", listOf(audio("content://music/other/x")))))

            val q = queue()
            q.setQueue("content://music/does-not-exist")

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `moveToNext on Active with hasNext advances currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveToNext()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(entries[1], state.current)
            assertTrue(state.hasPrevious)
            assertTrue(state.hasNext)
        }

    @Test
    fun `moveToNext on Active at last index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

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
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveToNext()
            q.moveToNext() // index 2
            q.moveToPrevious() // back to 1

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(entries[1], state.current)
        }

    @Test
    fun `moveToPrevious on Active at first index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

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
            mediaRepo.emit(listOf(ready(folderUri, listOf(audio("$folderUri/a")))))

            val q = queue()
            q.setQueue(folderUri)
            q.clear()

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `snapshot — repository change to same folder does not mutate active queue`() =
        runTest {
            val folderUri = "content://music/album"
            val initialEntries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, initialEntries)))

            val q = queue()
            q.setQueue(folderUri)

            // Repository now emits a *different* entry list for the same folder.
            val newEntries = listOf(audio("$folderUri/x"), audio("$folderUri/y"), audio("$folderUri/z"))
            mediaRepo.emit(listOf(ready(folderUri, newEntries)))

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(initialEntries, state.entries)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `snapshot — repository eviction of source folder does not mutate active queue`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)

            // Folder is removed from the repository entirely.
            mediaRepo.emit(emptyList())

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(entries, state.entries)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue replaces queue wholesale across folders`() =
        runTest {
            val uriA = "content://music/a"
            val uriB = "content://music/b"
            val entriesA = listOf(audio("$uriA/1"), audio("$uriA/2"))
            val entriesB = listOf(audio("$uriB/1"))
            mediaRepo.emit(listOf(ready(uriA, entriesA), ready(uriB, entriesB)))

            val q = queue()
            q.setQueue(uriA)
            q.moveToNext() // currentIndex = 1 in A
            q.setQueue(uriB)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(entriesB, state.entries)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri matching entry sets currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/b")

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(1, state.currentIndex)
            assertSame(entries[1], state.current)
        }

    @Test
    fun `setQueue with startTrackUri not matching any entry falls back to index 0`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/does-not-exist")

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri null uses index 0`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = null)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `setQueue with startTrackUri on unreachable folder yields Empty`() =
        runTest {
            val folderUri = "content://music/gone"
            mediaRepo.emit(listOf(unreachable(folderUri)))

            val q = queue()
            q.setQueue(folderUri, startTrackUri = "$folderUri/anything")

            assertEquals(PlaybackQueueState.Empty, q.state.value)
        }

    @Test
    fun `moveTo with valid index updates currentIndex`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(2)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(2, state.currentIndex)
            assertSame(entries[2], state.current)
        }

    @Test
    fun `moveTo with negative index is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

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
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value
            q.moveTo(entries.size)

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
            val entries = listOf(audio("$folderUri/a"), audio("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))

            val q = queue()
            q.setQueue(folderUri)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 2), state.playbackOrder)
            assertEquals(false, state.shuffleEnabled)
        }

    @Test
    fun `setShuffleEnabled true anchors current entry at order position 0 and permutes the rest`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (1..5).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
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
    fun `setShuffleEnabled false resets order to identity and keeps currentIndex on same entry`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (1..5).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.moveTo(2)
            q.setShuffleEnabled(enabled = true)
            val currentEntryBefore = (q.state.value as PlaybackQueueState.Active).current

            q.setShuffleEnabled(enabled = false)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 2, 3, 4), state.playbackOrder)
            assertEquals(false, state.shuffleEnabled)
            assertSame(currentEntryBefore, state.current)
        }

    @Test
    fun `moveToNext during shuffle traverses playback order not entries order`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (1..5).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
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
            val entries = (1..5).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
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
            val entries = (1..5).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
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
            val entries = (1..8).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)

            q.setShuffleEnabled(enabled = true)

            val state = q.state.value as PlaybackQueueState.Active
            assertNotEquals(entries.indices.toList(), state.playbackOrder)
        }

    // ---------------------------------------------------------------------
    // Editable queue — reorder (move)
    // ---------------------------------------------------------------------

    @Test
    fun `move reorders playbackOrder and keeps currentIndex on the same entry`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(1)

            q.move(0, 2)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(1, 2, 0, 3), state.playbackOrder)
            assertEquals(1, state.currentIndex)
            assertEquals(entries, state.entries)
        }

    @Test
    fun `move reorders across media kinds`() =
        runTest {
            val folderUri = "content://media/album"
            val entries = listOf(audio("$folderUri/a"), video("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)

            q.move(2, 0) // move the audio at the end before the video

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(2, 0, 1), state.playbackOrder)
        }

    @Test
    fun `move during shuffle reorders the shuffled order`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..4).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.setShuffleEnabled(enabled = true)
            val before = (q.state.value as PlaybackQueueState.Active).playbackOrder

            q.move(0, 1)

            val expected = before.toMutableList().also { it.add(1, it.removeAt(0)) }
            assertEquals(expected, (q.state.value as PlaybackQueueState.Active).playbackOrder)
        }

    @Test
    fun `move with out-of-bounds position is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..2).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value

            q.move(0, 99)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `move on Empty is no-op`() {
        val q = queue()
        q.move(0, 1)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    // ---------------------------------------------------------------------
    // Editable queue — deactivate
    // ---------------------------------------------------------------------

    @Test
    fun `deactivate non-current entry moves it to the deactivated section`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(2)

            q.deactivate(0)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(1, 2, 3), state.playbackOrder)
            assertEquals(listOf(0), state.deactivated)
            assertEquals(2, state.currentIndex)
        }

    @Test
    fun `deactivate a video entry between audio entries partitions correctly`() =
        runTest {
            val folderUri = "content://media/album"
            val entries = listOf(audio("$folderUri/a"), video("$folderUri/b"), audio("$folderUri/c"))
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)

            q.deactivate(1) // the video at order position 1

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 2), state.playbackOrder)
            assertEquals(listOf(1), state.deactivated)
            assertTrue(state.entries[state.deactivated.single()] is MediaEntry.Video)
        }

    @Test
    fun `deactivate current entry advances currentIndex to successor in order`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(1)

            q.deactivate(1)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 2, 3), state.playbackOrder)
            assertEquals(listOf(1), state.deactivated)
            assertEquals(2, state.currentIndex)
        }

    @Test
    fun `deactivate current entry at last active position falls back to predecessor`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..2).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(2)

            q.deactivate(2)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1), state.playbackOrder)
            assertEquals(listOf(2), state.deactivated)
            assertEquals(1, state.currentIndex)
        }

    @Test
    fun `deactivate the only remaining active entry is a no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..2).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.deactivate(0)
            q.deactivate(0)
            val before = q.state.value as PlaybackQueueState.Active
            assertEquals(1, before.playbackOrder.size)

            q.deactivate(0)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `deactivate appends to the back of the deactivated section`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(3)

            q.deactivate(0)
            q.deactivate(0)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1), state.deactivated)
            assertEquals(listOf(2, 3), state.playbackOrder)
        }

    @Test
    fun `deactivate with out-of-bounds position is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..2).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value

            q.deactivate(99)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `deactivate on Empty is no-op`() {
        val q = queue()
        q.deactivate(0)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    // ---------------------------------------------------------------------
    // Editable queue — moveAfterCurrent ("play next")
    // ---------------------------------------------------------------------

    @Test
    fun `moveAfterCurrent places entry right after current`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..4).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(1)

            q.moveAfterCurrent(3)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 3, 2, 4), state.playbackOrder)
            assertEquals(1, state.currentIndex)
        }

    @Test
    fun `moveAfterCurrent on the current entry itself is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(1)
            val before = q.state.value

            q.moveAfterCurrent(1)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `moveAfterCurrent when current is last appends directly after current`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(3)

            q.moveAfterCurrent(0)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(1, 2, 3, 0), state.playbackOrder)
            assertEquals(3, state.currentIndex)
        }

    @Test
    fun `moveAfterCurrent on Empty is no-op`() {
        val q = queue()
        q.moveAfterCurrent(0)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    // ---------------------------------------------------------------------
    // Editable queue — reactivate
    // ---------------------------------------------------------------------

    @Test
    fun `reactivate appends a deactivated entry to the end of the active order`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.deactivate(1)

            q.reactivate(1)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 2, 3, 1), state.playbackOrder)
            assertEquals(emptyList<Int>(), state.deactivated)
            assertEquals(0, state.currentIndex)
        }

    @Test
    fun `reactivate removes only the chosen entry from the deactivated section`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(3)
            q.deactivate(0) // deactivate entry 0
            q.deactivate(0) // deactivate entry 1

            q.reactivate(1)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0), state.deactivated)
            assertEquals(1, state.playbackOrder.last())
        }

    @Test
    fun `reactivateAt inserts a deactivated entry at the given active position`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(3)
            q.deactivate(0) // order [1,2,3], deactivated [0]

            q.reactivateAt(0, 1)

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(1, 0, 2, 3), state.playbackOrder)
            assertEquals(emptyList<Int>(), state.deactivated)
        }

    @Test
    fun `reactivate of a non-deactivated entry is no-op`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..3).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            val before = q.state.value

            q.reactivate(0)

            assertEquals(before, q.state.value)
        }

    @Test
    fun `reactivate on Empty is no-op`() {
        val q = queue()
        q.reactivate(0)
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    // ---------------------------------------------------------------------
    // Editable queue — reset
    // ---------------------------------------------------------------------

    @Test
    fun `reset restores natural active order, clears deactivated, and disables shuffle`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..4).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue(Random(42L))
            q.setQueue(folderUri)
            q.setShuffleEnabled(enabled = true)
            q.deactivate(2)

            q.reset()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(listOf(0, 1, 2, 3, 4), state.playbackOrder)
            assertEquals(emptyList<Int>(), state.deactivated)
            assertEquals(false, state.shuffleEnabled)
        }

    @Test
    fun `reset keeps the current entry`() =
        runTest {
            val folderUri = "content://music/album"
            val entries = (0..4).map { audio("$folderUri/$it") }
            mediaRepo.emit(listOf(ready(folderUri, entries)))
            val q = queue()
            q.setQueue(folderUri)
            q.moveTo(3)

            q.reset()

            val state = q.state.value as PlaybackQueueState.Active
            assertEquals(3, state.currentIndex)
        }

    @Test
    fun `reset on Empty is no-op`() {
        val q = queue()
        q.reset()
        assertEquals(PlaybackQueueState.Empty, q.state.value)
    }

    // ---------------------------------------------------------------------
    // Active partition invariant (ADR-008/ADR-010): active ∪ deactivated covers every index once
    // ---------------------------------------------------------------------

    @Test
    fun `Active accepts a partition of active and deactivated indices`() {
        val folderUri = "content://music/album"
        val entries = (0..3).map { audio("$folderUri/$it") }

        val active =
            PlaybackQueueState.Active(
                entries = entries,
                currentIndex = 2,
                playbackOrder = listOf(0, 2, 3),
                deactivated = listOf(1),
            )

        assertEquals(listOf(0, 2, 3), active.playbackOrder)
        assertEquals(listOf(1), active.deactivated)
        assertSame(entries[2], active.current)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Active rejects an entry index present in both active and deactivated`() {
        val entries = (0..2).map { audio("content://music/album/$it") }
        PlaybackQueueState.Active(
            entries = entries,
            currentIndex = 0,
            playbackOrder = listOf(0, 1),
            deactivated = listOf(1, 2),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Active rejects when an entry index is in neither list`() {
        val entries = (0..2).map { audio("content://music/album/$it") }
        PlaybackQueueState.Active(
            entries = entries,
            currentIndex = 0,
            playbackOrder = listOf(0),
            deactivated = listOf(1),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Active rejects currentIndex that is not an active entry`() {
        val entries = (0..2).map { audio("content://music/album/$it") }
        PlaybackQueueState.Active(
            entries = entries,
            currentIndex = 1,
            playbackOrder = listOf(0, 2),
            deactivated = listOf(1),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Active rejects an empty active order`() {
        val entries = (0..1).map { audio("content://music/album/$it") }
        PlaybackQueueState.Active(
            entries = entries,
            currentIndex = 0,
            playbackOrder = emptyList(),
            deactivated = listOf(0, 1),
        )
    }
}
