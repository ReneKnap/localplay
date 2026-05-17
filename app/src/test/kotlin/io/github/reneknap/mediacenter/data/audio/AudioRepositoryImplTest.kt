package io.github.reneknap.mediacenter.data.audio

import app.cash.turbine.test
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRepositoryImplTest {
    private lateinit var folderRepo: FakeFolderRepository
    private lateinit var scanner: FakeAudioFileScanner
    private lateinit var tagReader: FakeTagReader

    @Before
    fun setUp() {
        folderRepo = FakeFolderRepository()
        scanner = FakeAudioFileScanner()
        tagReader = FakeTagReader()
    }

    private fun TestScope.repository(): AudioRepositoryImpl =
        AudioRepositoryImpl(
            folderRepository = folderRepo,
            scanner = scanner,
            tagReader = tagReader,
            scope = backgroundScope,
        )

    @Test
    fun `folders flow emits empty list when no folders picked`() =
        runTest {
            val repo = repository()

            repo.folders.test {
                assertEquals(emptyList<FolderTracks>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reachable folder transitions from Scanning to Ready`() =
        runTest {
            val folderUri = "content://music/album"
            val entry = FolderEntry(folderUri, "Album", isReachable = true)
            folderRepo.emit(listOf(entry))

            val repo = repository()

            repo.folders.test {
                val scanning = awaitItem()
                assertEquals(1, scanning.size)
                assertEquals(entry, scanning[0].folder)
                assertEquals(FolderScanState.Scanning, scanning[0].scan)

                val file =
                    RawAudioFile(
                        uri = "content://music/album/song.mp3",
                        displayName = "song.mp3",
                        mimeType = "audio/mpeg",
                        sizeBytes = 1_000L,
                    )
                tagReader.setTags(
                    file.uri,
                    AudioTags(title = "Song", artist = "Artist", album = "Album", durationMs = 30_000L),
                )
                scanner.setResult(folderUri, listOf(file))

                val ready = awaitItem()
                val state = ready[0].scan
                assertTrue(state is FolderScanState.Ready)
                val tracks = (state as FolderScanState.Ready).tracks
                assertEquals(1, tracks.size)
                assertEquals("Song", tracks[0].title)
                assertEquals("Artist", tracks[0].artist)
                assertEquals(30_000L, tracks[0].durationMs)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tracks within a folder are sorted by displayName case-insensitive`() =
        runTest {
            val folderUri = "content://music/mixed"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Mixed", isReachable = true)))

            val files =
                listOf(
                    RawAudioFile("$folderUri/zebra.mp3", "Zebra.mp3", "audio/mpeg", 0L),
                    RawAudioFile("$folderUri/apple.mp3", "apple.mp3", "audio/mpeg", 0L),
                    RawAudioFile("$folderUri/banana.mp3", "Banana.mp3", "audio/mpeg", 0L),
                )
            scanner.setResult(folderUri, files)

            val repo = repository()

            repo.folders.test {
                awaitItem() // skip Scanning
                val ready = awaitItem()
                val tracks = (ready[0].scan as FolderScanState.Ready).tracks
                assertEquals(
                    listOf("apple.mp3", "Banana.mp3", "Zebra.mp3"),
                    tracks.map { it.displayName },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `track title falls back to filename without extension when tag title is null`() =
        runTest {
            val folderUri = "content://music/tagless"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Tagless", isReachable = true)))

            val file = RawAudioFile("$folderUri/Untitled Track.flac", "Untitled Track.flac", "audio/flac", 0L)
            scanner.setResult(folderUri, listOf(file))
            // No tags configured for the file — falls back to AudioTags.EMPTY.

            val repo = repository()

            repo.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()
                val tracks = (ready[0].scan as FolderScanState.Ready).tracks
                assertEquals("Untitled Track", tracks[0].title)
                assertNull(tracks[0].artist)
                assertNull(tracks[0].album)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `track duration falls back to zero when tag duration is null`() =
        runTest {
            val folderUri = "content://music/durationless"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Dur", isReachable = true)))

            val file = RawAudioFile("$folderUri/song.mp3", "song.mp3", "audio/mpeg", 0L)
            scanner.setResult(folderUri, listOf(file))
            tagReader.setTags(file.uri, AudioTags(title = "Song", artist = null, album = null, durationMs = null))

            val repo = repository()

            repo.folders.test {
                awaitItem()
                val ready = awaitItem()
                val tracks = (ready[0].scan as FolderScanState.Ready).tracks
                assertEquals(0L, tracks[0].durationMs)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `scan failure transitions folder to Unreachable instead of staying on Scanning`() =
        runTest {
            val folderUri = "content://music/broken"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Broken", isReachable = true)))
            scanner.failWith(folderUri, RuntimeException("simulated scan failure"))

            val repo = repository()

            repo.folders.test {
                var emit = awaitItem()
                while (emit[0].scan is FolderScanState.Scanning) {
                    emit = awaitItem()
                }
                assertEquals(FolderScanState.Unreachable, emit[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unreachable folder yields Unreachable state without invoking scanner`() =
        runTest {
            val folderUri = "content://music/deleted"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Deleted", isReachable = false)))

            val repo = repository()

            repo.folders.test {
                // Scanning may briefly appear before syncScans runs; skip until terminal state.
                var emit = awaitItem()
                while (emit.isNotEmpty() && emit[0].scan is FolderScanState.Scanning) {
                    emit = awaitItem()
                }
                assertEquals(FolderScanState.Unreachable, emit[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
            assertFalse(scanner.scannedUris.contains(folderUri))
        }

    @Test
    fun `folder with no audio files yields Ready with empty list`() =
        runTest {
            val folderUri = "content://music/empty"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Empty", isReachable = true)))
            scanner.setResult(folderUri, emptyList())

            val repo = repository()

            repo.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()
                val state = ready[0].scan
                assertTrue(state is FolderScanState.Ready)
                assertTrue((state as FolderScanState.Ready).tracks.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `removing a folder evicts its scan state`() =
        runTest {
            val uriA = "content://music/a"
            val uriB = "content://music/b"
            folderRepo.emit(
                listOf(
                    FolderEntry(uriA, "A", isReachable = true),
                    FolderEntry(uriB, "B", isReachable = true),
                ),
            )
            scanner.setResult(uriA, listOf(RawAudioFile("$uriA/1.mp3", "1.mp3", "audio/mpeg", 0L)))
            scanner.setResult(uriB, listOf(RawAudioFile("$uriB/1.mp3", "1.mp3", "audio/mpeg", 0L)))

            val repo = repository()

            repo.folders.test {
                var emit = awaitItem()
                while (emit.size != 2 || emit.any { it.scan !is FolderScanState.Ready }) {
                    emit = awaitItem()
                }

                folderRepo.emit(listOf(FolderEntry(uriB, "B", isReachable = true)))

                val after = awaitItem()
                assertEquals(1, after.size)
                assertEquals(uriB, after[0].folder.uri)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `re-emitting the same folder list does not rescan cached folders`() =
        runTest {
            val folderUri = "content://music/cached"
            val entry = FolderEntry(folderUri, "Cached", isReachable = true)
            folderRepo.emit(listOf(entry))
            scanner.setResult(folderUri, listOf(RawAudioFile("$folderUri/1.mp3", "1.mp3", "audio/mpeg", 0L)))

            val repo = repository()

            repo.folders.test {
                var emit = awaitItem()
                while (emit[0].scan !is FolderScanState.Ready) {
                    emit = awaitItem()
                }
                cancelAndIgnoreRemainingEvents()
            }

            // Re-emit identical list — should not trigger a second scan.
            folderRepo.emit(listOf(entry))
            runCurrent()

            assertEquals(1, scanner.scannedUris.count { it == folderUri })
        }

    @Test
    fun `multiple folders are scanned independently`() =
        runTest {
            val uriA = "content://music/a"
            val uriB = "content://music/b"
            folderRepo.emit(
                listOf(
                    FolderEntry(uriA, "A", isReachable = true),
                    FolderEntry(uriB, "B", isReachable = true),
                ),
            )
            // Only A is resolved initially; B stays in Scanning.
            scanner.setResult(uriA, listOf(RawAudioFile("$uriA/1.mp3", "1.mp3", "audio/mpeg", 0L)))

            val repo = repository()

            repo.folders.test {
                var emit = awaitItem()
                // Wait until A is Ready while B remains Scanning.
                while (true) {
                    val a = emit.first { it.folder.uri == uriA }
                    val b = emit.first { it.folder.uri == uriB }
                    if (a.scan is FolderScanState.Ready && b.scan is FolderScanState.Scanning) break
                    emit = awaitItem()
                }

                // Now resolve B.
                scanner.setResult(uriB, listOf(RawAudioFile("$uriB/1.mp3", "1.mp3", "audio/mpeg", 0L)))

                emit = awaitItem()
                while (emit.first { it.folder.uri == uriB }.scan !is FolderScanState.Ready) {
                    emit = awaitItem()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
}
