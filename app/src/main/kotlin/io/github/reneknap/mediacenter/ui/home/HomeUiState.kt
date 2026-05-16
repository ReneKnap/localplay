package io.github.reneknap.mediacenter.ui.home

import io.github.reneknap.mediacenter.data.folder.FolderEntry

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Folders(val items: List<FolderEntry>) : HomeUiState
}
