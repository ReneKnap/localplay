package io.github.reneknap.mediacenter.data.video

import io.github.reneknap.mediacenter.data.media.MediaScanIndex
import io.github.reneknap.mediacenter.data.media.MediaScanState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl
    @Inject
    constructor(
        private val mediaScanIndex: MediaScanIndex,
    ) : VideoRepository {
        override val folders: Flow<List<FolderVideos>> =
            mediaScanIndex.folders.map { mediaFolders ->
                mediaFolders.map { media ->
                    FolderVideos(media.folder, media.scan.toVideoScanState())
                }
            }
    }

private fun MediaScanState.toVideoScanState(): VideoScanState =
    when (this) {
        MediaScanState.Scanning -> VideoScanState.Scanning
        MediaScanState.Unreachable -> VideoScanState.Unreachable
        is MediaScanState.Ready -> VideoScanState.Ready(video)
    }
