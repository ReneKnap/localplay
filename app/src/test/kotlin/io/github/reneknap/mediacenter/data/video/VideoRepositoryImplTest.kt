package io.github.reneknap.mediacenter.data.video

import app.cash.turbine.test
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.media.FakeMediaScanIndex
import io.github.reneknap.mediacenter.data.media.FolderMedia
import io.github.reneknap.mediacenter.data.media.MediaScanState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryImplTest {
    private val entry = FolderEntry("content://media/folder", "Folder", isReachable = true)

    private fun videoItem(name: String) =
        VideoItem(
            uri = "${entry.uri}/$name",
            folderUri = entry.uri,
            displayName = name,
            mimeType = "video/mp4",
            sizeBytes = 0L,
            durationMs = 1_000L,
            width = 640,
            height = 480,
        )

    private fun audioTrack(name: String) =
        AudioTrack(
            uri = "${entry.uri}/$name",
            folderUri = entry.uri,
            displayName = name,
            mimeType = "audio/mpeg",
            sizeBytes = 0L,
            title = name,
            artist = null,
            album = null,
            durationMs = 0L,
        )

    @Test
    fun `empty index yields empty list`() =
        runTest {
            val repo = VideoRepositoryImpl(FakeMediaScanIndex())

            repo.folders.test {
                assertEquals(emptyList<FolderVideos>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Scanning media state projects to Scanning video state`() =
        runTest {
            val index = FakeMediaScanIndex(listOf(FolderMedia(entry, MediaScanState.Scanning)))
            val repo = VideoRepositoryImpl(index)

            repo.folders.test {
                val folders = awaitItem()
                assertEquals(1, folders.size)
                assertEquals(entry, folders[0].folder)
                assertEquals(VideoScanState.Scanning, folders[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Ready media state projects only the video list`() =
        runTest {
            val ready =
                MediaScanState.Ready(
                    audio = listOf(audioTrack("song.mp3")),
                    video = listOf(videoItem("clip.mp4")),
                )
            val index = FakeMediaScanIndex(listOf(FolderMedia(entry, ready)))
            val repo = VideoRepositoryImpl(index)

            repo.folders.test {
                val state = awaitItem()[0].scan
                assertTrue(state is VideoScanState.Ready)
                state as VideoScanState.Ready
                assertEquals(listOf("clip.mp4"), state.videos.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Unreachable media state projects to Unreachable video state`() =
        runTest {
            val index = FakeMediaScanIndex(listOf(FolderMedia(entry, MediaScanState.Unreachable)))
            val repo = VideoRepositoryImpl(index)

            repo.folders.test {
                assertEquals(VideoScanState.Unreachable, awaitItem()[0].scan)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
