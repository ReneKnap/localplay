package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.folder.FolderEntry

data class FolderTracks(
    val folder: FolderEntry,
    val scan: FolderScanState,
)
