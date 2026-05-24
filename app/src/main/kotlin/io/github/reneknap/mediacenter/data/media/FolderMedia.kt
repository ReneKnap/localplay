package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.folder.FolderEntry

data class FolderMedia(
    val folder: FolderEntry,
    val scan: MediaScanState,
)
