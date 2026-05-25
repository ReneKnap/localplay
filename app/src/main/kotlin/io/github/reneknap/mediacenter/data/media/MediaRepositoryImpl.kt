package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.video.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl
    @Inject
    constructor(
        private val mediaScanIndex: MediaScanIndex,
    ) : MediaRepository {
        override val folders: Flow<List<FolderMediaContent>> =
            mediaScanIndex.folders.map { mediaFolders ->
                mediaFolders.map { media ->
                    FolderMediaContent(media.folder, media.scan.toContentScanState())
                }
            }
    }

private fun MediaScanState.toContentScanState(): MediaContentScanState =
    when (this) {
        MediaScanState.Scanning -> MediaContentScanState.Scanning
        MediaScanState.Unreachable -> MediaContentScanState.Unreachable
        is MediaScanState.Ready -> MediaContentScanState.Ready(combinedEntries(audio, video))
    }

/**
 * Merge audio and video into one folder-order list: sorted by display name (case-insensitive) so the
 * two kinds interleave alphabetically, with the uri as a stable tie-break.
 */
private fun combinedEntries(
    audio: List<AudioTrack>,
    video: List<VideoItem>,
): List<MediaEntry> {
    val entries = audio.map(MediaEntry::Audio) + video.map(MediaEntry::Video)
    return entries.sortedWith(compareBy({ it.displayName.lowercase() }, { it.uri }))
}
