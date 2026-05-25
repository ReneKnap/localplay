package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.video.VideoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImplTest {
    private fun track(
        folderUri: String,
        name: String,
        uri: String = "$folderUri/$name",
    ): AudioTrack =
        AudioTrack(
            uri = uri,
            folderUri = folderUri,
            displayName = name,
            mimeType = "audio/mpeg",
            sizeBytes = 0L,
            title = name.substringBeforeLast('.'),
            artist = null,
            album = null,
            durationMs = 0L,
        )

    private fun video(
        folderUri: String,
        name: String,
        uri: String = "$folderUri/$name",
    ): VideoItem =
        VideoItem(
            uri = uri,
            folderUri = folderUri,
            displayName = name,
            mimeType = "video/mp4",
            sizeBytes = 0L,
            durationMs = 0L,
            width = 0,
            height = 0,
        )

    private fun folderMedia(
        folderUri: String,
        scan: MediaScanState,
    ): FolderMedia =
        FolderMedia(
            folder =
                FolderEntry(
                    folderUri,
                    folderUri.substringAfterLast('/'),
                    isReachable = scan !is MediaScanState.Unreachable,
                ),
            scan = scan,
        )

    private fun repository(folders: List<FolderMedia>): MediaRepositoryImpl =
        MediaRepositoryImpl(FakeMediaScanIndex(folders))

    @Test
    fun `ready folder with audio and video yields one combined entries list`() =
        runTest {
            val f = "content://m/album"
            val repo =
                repository(
                    listOf(
                        folderMedia(
                            f,
                            MediaScanState.Ready(
                                audio = listOf(track(f, "song.mp3")),
                                video = listOf(video(f, "clip.mp4")),
                            ),
                        ),
                    ),
                )

            val scan = repo.folders.first().single().scan as MediaContentScanState.Ready

            assertEquals(2, scan.entries.size)
        }

    @Test
    fun `combined entries are sorted by displayName case-insensitive`() =
        runTest {
            val f = "content://m/album"
            val repo =
                repository(
                    listOf(
                        folderMedia(
                            f,
                            MediaScanState.Ready(
                                audio = listOf(track(f, "Banana.mp3"), track(f, "apple.mp3")),
                                video = listOf(video(f, "Cherry.mp4")),
                            ),
                        ),
                    ),
                )

            val scan = repo.folders.first().single().scan as MediaContentScanState.Ready

            assertEquals(listOf("apple.mp3", "Banana.mp3", "Cherry.mp4"), scan.entries.map { it.displayName })
        }

    @Test
    fun `entries with equal displayName fall back to uri order`() =
        runTest {
            val f = "content://m/album"
            val repo =
                repository(
                    listOf(
                        folderMedia(
                            f,
                            MediaScanState.Ready(
                                audio = listOf(track(f, "same.x", uri = "$f/zzz")),
                                video = listOf(video(f, "same.x", uri = "$f/aaa")),
                            ),
                        ),
                    ),
                )

            val scan = repo.folders.first().single().scan as MediaContentScanState.Ready

            assertEquals(listOf("$f/aaa", "$f/zzz"), scan.entries.map { it.uri })
        }

    @Test
    fun `audio-only folder yields only Audio entries`() =
        runTest {
            val f = "content://m/music"
            val repo =
                repository(
                    listOf(
                        folderMedia(f, MediaScanState.Ready(audio = listOf(track(f, "a.mp3")), video = emptyList())),
                    ),
                )

            val scan = repo.folders.first().single().scan as MediaContentScanState.Ready

            assertTrue(scan.entries.all { it is MediaEntry.Audio })
        }

    @Test
    fun `video-only folder yields only Video entries`() =
        runTest {
            val f = "content://m/clips"
            val repo =
                repository(
                    listOf(
                        folderMedia(f, MediaScanState.Ready(audio = emptyList(), video = listOf(video(f, "v.mp4")))),
                    ),
                )

            val scan = repo.folders.first().single().scan as MediaContentScanState.Ready

            assertTrue(scan.entries.all { it is MediaEntry.Video })
        }

    @Test
    fun `scanning folder yields Scanning content state`() =
        runTest {
            val f = "content://m/scanning"
            val repo = repository(listOf(folderMedia(f, MediaScanState.Scanning)))

            assertEquals(MediaContentScanState.Scanning, repo.folders.first().single().scan)
        }

    @Test
    fun `unreachable folder yields Unreachable content state`() =
        runTest {
            val f = "content://m/gone"
            val repo = repository(listOf(folderMedia(f, MediaScanState.Unreachable)))

            assertEquals(MediaContentScanState.Unreachable, repo.folders.first().single().scan)
        }

    @Test
    fun `multiple folders are projected independently`() =
        runTest {
            val music = "content://m/music"
            val clips = "content://m/clips"
            val repo =
                repository(
                    listOf(
                        folderMedia(music, MediaScanState.Ready(audio = listOf(track(music, "a.mp3")), video = emptyList())),
                        folderMedia(clips, MediaScanState.Scanning),
                    ),
                )

            val folders = repo.folders.first()

            assertEquals(2, folders.size)
            assertTrue(folders[0].scan is MediaContentScanState.Ready)
            assertEquals(MediaContentScanState.Scanning, folders[1].scan)
        }

    @Test
    fun `folder eviction removes the projection`() =
        runTest {
            val f = "content://m/album"
            val index =
                FakeMediaScanIndex(
                    listOf(folderMedia(f, MediaScanState.Ready(audio = listOf(track(f, "a.mp3")), video = emptyList()))),
                )
            val repo = MediaRepositoryImpl(index)
            assertEquals(1, repo.folders.first().size)

            index.emit(emptyList())

            assertTrue(repo.folders.first().isEmpty())
        }

    @Test
    fun `MediaEntry Audio delegates common properties to the track`() {
        val t = track("content://m/album", "song.mp3")
        val entry = MediaEntry.Audio(t)

        assertEquals(t.uri, entry.uri)
        assertEquals(t.folderUri, entry.folderUri)
        assertEquals(t.displayName, entry.displayName)
        assertEquals(t.durationMs, entry.durationMs)
        assertEquals(MediaKind.AUDIO, entry.kind)
    }

    @Test
    fun `MediaEntry Video delegates common properties to the video`() {
        val v = video("content://m/album", "clip.mp4")
        val entry = MediaEntry.Video(v)

        assertEquals(v.uri, entry.uri)
        assertEquals(v.folderUri, entry.folderUri)
        assertEquals(v.displayName, entry.displayName)
        assertEquals(v.durationMs, entry.durationMs)
        assertEquals(MediaKind.VIDEO, entry.kind)
    }
}
