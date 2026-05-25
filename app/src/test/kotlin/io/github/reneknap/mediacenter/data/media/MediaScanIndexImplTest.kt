package io.github.reneknap.mediacenter.data.media

import app.cash.turbine.test
import io.github.reneknap.mediacenter.data.audio.AudioTags
import io.github.reneknap.mediacenter.data.audio.FakeTagReader
import io.github.reneknap.mediacenter.data.folder.FakeFolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.video.FakeVideoMetadataReader
import io.github.reneknap.mediacenter.data.video.VideoMetadata
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
class MediaScanIndexImplTest {
    private lateinit var folderRepo: FakeFolderRepository
    private lateinit var scanner: FakeMediaFileScanner
    private lateinit var tagReader: FakeTagReader
    private lateinit var videoMetadataReader: FakeVideoMetadataReader

    @Before
    fun setUp() {
        folderRepo = FakeFolderRepository()
        scanner = FakeMediaFileScanner()
        tagReader = FakeTagReader()
        videoMetadataReader = FakeVideoMetadataReader()
    }

    private fun TestScope.index(): MediaScanIndexImpl =
        MediaScanIndexImpl(
            folderRepository = folderRepo,
            scanner = scanner,
            tagReader = tagReader,
            videoMetadataReader = videoMetadataReader,
            scope = backgroundScope,
        )

    private fun audioRaw(
        folderUri: String,
        name: String,
    ) = RawMediaFile("$folderUri/$name", name, "audio/mpeg", 0L, MediaKind.AUDIO, parentKey = folderUri)

    private fun videoRaw(
        folderUri: String,
        name: String,
    ) = RawMediaFile("$folderUri/$name", name, "video/mp4", 0L, MediaKind.VIDEO, parentKey = folderUri)

    private fun subRaw(
        folderUri: String,
        name: String,
        parentKey: String = folderUri,
    ) = RawSubtitleFile("$parentKey/$name", name, parentKey)

    @Test
    fun `folders flow emits empty list when no folders picked`() =
        runTest {
            val index = index()

            index.folders.test {
                assertEquals(emptyList<FolderMedia>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reachable folder transitions from Scanning to Ready with audio and video`() =
        runTest {
            val folderUri = "content://media/mixed"
            val entry = FolderEntry(folderUri, "Mixed", isReachable = true)
            folderRepo.emit(listOf(entry))

            val index = index()

            index.folders.test {
                val scanning = awaitItem()
                assertEquals(1, scanning.size)
                assertEquals(entry, scanning[0].folder)
                assertEquals(MediaScanState.Scanning, scanning[0].scan)

                val audio = audioRaw(folderUri, "song.mp3")
                val video = videoRaw(folderUri, "clip.mp4")
                tagReader.setTags(
                    audio.uri,
                    AudioTags(title = "Song", artist = "Artist", album = "Album", durationMs = 30_000L),
                )
                videoMetadataReader.setMetadata(video.uri, VideoMetadata(durationMs = 90_000L, width = 1920, height = 1080))
                scanner.setResult(folderUri, listOf(audio, video))

                val ready = awaitItem()
                val state = ready[0].scan
                assertTrue(state is MediaScanState.Ready)
                state as MediaScanState.Ready

                assertEquals(1, state.audio.size)
                assertEquals("Song", state.audio[0].title)
                assertEquals("Artist", state.audio[0].artist)
                assertEquals(30_000L, state.audio[0].durationMs)

                assertEquals(1, state.video.size)
                assertEquals("clip.mp4", state.video[0].displayName)
                assertEquals(folderUri, state.video[0].folderUri)
                assertEquals(90_000L, state.video[0].durationMs)
                assertEquals(1920, state.video[0].width)
                assertEquals(1080, state.video[0].height)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `audio is sorted by displayName case-insensitive`() =
        runTest {
            val folderUri = "content://media/audio"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Audio", isReachable = true)))
            scanner.setResult(
                folderUri,
                listOf(
                    audioRaw(folderUri, "Zebra.mp3"),
                    audioRaw(folderUri, "apple.mp3"),
                    audioRaw(folderUri, "Banana.mp3"),
                ),
            )

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertEquals(
                    listOf("apple.mp3", "Banana.mp3", "Zebra.mp3"),
                    ready.audio.map { it.displayName },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `video is sorted by displayName case-insensitive`() =
        runTest {
            val folderUri = "content://media/video"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Video", isReachable = true)))
            val files =
                listOf(
                    videoRaw(folderUri, "Zoo.mp4"),
                    videoRaw(folderUri, "alpha.mkv"),
                    videoRaw(folderUri, "Beta.webm"),
                )
            files.forEach { videoMetadataReader.setMetadata(it.uri, VideoMetadata(1L, 640, 480)) }
            scanner.setResult(folderUri, files)

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertEquals(
                    listOf("alpha.mkv", "Beta.webm", "Zoo.mp4"),
                    ready.video.map { it.displayName },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `audio title falls back to filename without extension when tag title is null`() =
        runTest {
            val folderUri = "content://media/tagless"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Tagless", isReachable = true)))
            scanner.setResult(folderUri, listOf(audioRaw(folderUri, "Untitled Track.flac")))
            // No tags configured — FakeTagReader returns AudioTags.EMPTY.

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertEquals("Untitled Track", ready.audio[0].title)
                assertNull(ready.audio[0].artist)
                assertNull(ready.audio[0].album)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `audio duration falls back to zero when tag duration is null`() =
        runTest {
            val folderUri = "content://media/durationless"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Dur", isReachable = true)))
            val audio = audioRaw(folderUri, "song.mp3")
            scanner.setResult(folderUri, listOf(audio))
            tagReader.setTags(audio.uri, AudioTags(title = "Song", artist = null, album = null, durationMs = null))

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertEquals(0L, ready.audio[0].durationMs)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `video with unreadable metadata is still listed with unknown duration and resolution`() =
        runTest {
            val folderUri = "content://media/avi"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Avi", isReachable = true)))
            val readable = videoRaw(folderUri, "modern.mp4")
            val unreadable = videoRaw(folderUri, "legacy.avi")
            videoMetadataReader.setMetadata(readable.uri, VideoMetadata(5_000L, 1280, 720))
            // unreadable: no metadata set -> reader returns null -> still listed, with unknown
            // duration/resolution (older AVI codecs MediaMetadataRetriever cannot parse).
            scanner.setResult(folderUri, listOf(readable, unreadable))

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertEquals(listOf("legacy.avi", "modern.mp4"), ready.video.map { it.displayName })
                val legacy = ready.video.first { it.displayName == "legacy.avi" }
                assertEquals(0L, legacy.durationMs)
                assertEquals(0, legacy.width)
                assertEquals(0, legacy.height)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `scan failure transitions folder to Unreachable instead of staying on Scanning`() =
        runTest {
            val folderUri = "content://media/broken"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Broken", isReachable = true)))
            scanner.failWith(folderUri, RuntimeException("simulated scan failure"))

            val index = index()

            index.folders.test {
                var emit = awaitItem()
                while (emit[0].scan is MediaScanState.Scanning) {
                    emit = awaitItem()
                }
                assertEquals(MediaScanState.Unreachable, emit[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unreachable folder yields Unreachable state without invoking scanner`() =
        runTest {
            val folderUri = "content://media/deleted"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Deleted", isReachable = false)))

            val index = index()

            index.folders.test {
                var emit = awaitItem()
                while (emit.isNotEmpty() && emit[0].scan is MediaScanState.Scanning) {
                    emit = awaitItem()
                }
                assertEquals(MediaScanState.Unreachable, emit[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
            assertFalse(scanner.scannedUris.contains(folderUri))
        }

    @Test
    fun `folder with no media files yields Ready with empty lists`() =
        runTest {
            val folderUri = "content://media/empty"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Empty", isReachable = true)))
            scanner.setResult(folderUri, emptyList())

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val state = awaitItem()[0].scan
                assertTrue(state is MediaScanState.Ready)
                state as MediaScanState.Ready
                assertTrue(state.audio.isEmpty())
                assertTrue(state.video.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `removing a folder evicts its scan state`() =
        runTest {
            val uriA = "content://media/a"
            val uriB = "content://media/b"
            folderRepo.emit(
                listOf(
                    FolderEntry(uriA, "A", isReachable = true),
                    FolderEntry(uriB, "B", isReachable = true),
                ),
            )
            scanner.setResult(uriA, listOf(audioRaw(uriA, "1.mp3")))
            scanner.setResult(uriB, listOf(audioRaw(uriB, "1.mp3")))

            val index = index()

            index.folders.test {
                var emit = awaitItem()
                while (emit.size != 2 || emit.any { it.scan !is MediaScanState.Ready }) {
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
            val folderUri = "content://media/cached"
            val entry = FolderEntry(folderUri, "Cached", isReachable = true)
            folderRepo.emit(listOf(entry))
            scanner.setResult(folderUri, listOf(audioRaw(folderUri, "1.mp3")))

            val index = index()

            index.folders.test {
                var emit = awaitItem()
                while (emit[0].scan !is MediaScanState.Ready) {
                    emit = awaitItem()
                }
                cancelAndIgnoreRemainingEvents()
            }

            folderRepo.emit(listOf(entry))
            runCurrent()

            assertEquals(1, scanner.scannedUris.count { it == folderUri })
        }

    @Test
    fun `video gets its matched external subtitles and excludes foreign sidecars`() =
        runTest {
            val folderUri = "content://media/subs"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Subs", isReachable = true)))
            val video = videoRaw(folderUri, "film.mp4")
            videoMetadataReader.setMetadata(video.uri, VideoMetadata(1L, 640, 480))
            scanner.setResult(
                folderUri,
                listOf(video),
                subtitles = listOf(subRaw(folderUri, "film.de.srt"), subRaw(folderUri, "other.srt")),
            )

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                val subs = ready.video.single().externalSubtitles
                assertEquals(1, subs.size)
                assertEquals("de", subs[0].language)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `subtitle in a different directory is not matched`() =
        runTest {
            val folderUri = "content://media/dirs"
            folderRepo.emit(listOf(FolderEntry(folderUri, "Dirs", isReachable = true)))
            val video = videoRaw(folderUri, "film.mp4")
            videoMetadataReader.setMetadata(video.uri, VideoMetadata(1L, 640, 480))
            scanner.setResult(
                folderUri,
                listOf(video),
                subtitles = listOf(subRaw(folderUri, "film.srt", parentKey = "$folderUri/nested")),
            )

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertTrue(ready.video.single().externalSubtitles.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `subtitle files are not listed as media entries`() =
        runTest {
            val folderUri = "content://media/onlysubs"
            folderRepo.emit(listOf(FolderEntry(folderUri, "OnlySubs", isReachable = true)))
            scanner.setResult(folderUri, emptyList(), subtitles = listOf(subRaw(folderUri, "loose.srt")))

            val index = index()

            index.folders.test {
                awaitItem() // Scanning
                val ready = awaitItem()[0].scan as MediaScanState.Ready
                assertTrue(ready.audio.isEmpty())
                assertTrue(ready.video.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `multiple folders are scanned independently`() =
        runTest {
            val uriA = "content://media/a"
            val uriB = "content://media/b"
            folderRepo.emit(
                listOf(
                    FolderEntry(uriA, "A", isReachable = true),
                    FolderEntry(uriB, "B", isReachable = true),
                ),
            )
            // Only A is resolved initially; B stays in Scanning.
            scanner.setResult(uriA, listOf(audioRaw(uriA, "1.mp3")))

            val index = index()

            index.folders.test {
                var emit = awaitItem()
                while (true) {
                    val a = emit.first { it.folder.uri == uriA }
                    val b = emit.first { it.folder.uri == uriB }
                    if (a.scan is MediaScanState.Ready && b.scan is MediaScanState.Scanning) break
                    emit = awaitItem()
                }

                scanner.setResult(uriB, listOf(audioRaw(uriB, "1.mp3")))

                emit = awaitItem()
                while (emit.first { it.folder.uri == uriB }.scan !is MediaScanState.Ready) {
                    emit = awaitItem()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
}
