package io.github.reneknap.mediacenter.data.video

import io.github.reneknap.mediacenter.data.folder.FolderEntry

data class FolderVideos(
    val folder: FolderEntry,
    val scan: VideoScanState,
)
