package io.github.reneknap.mediacenter.ui.home

import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.media.MediaContentScanState

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    data class Folders(val items: List<FolderMediaUi>) : HomeUiState
}

data class FolderMediaUi(
    val folder: FolderEntry,
    val content: MediaContentScanState,
)
