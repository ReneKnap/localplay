package io.github.reneknap.mediacenter.ui.player

import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.playback.PlayerStatus

sealed interface FolderPlayerUiState {
    data object Loading : FolderPlayerUiState

    data object NotAvailable : FolderPlayerUiState

    data class EmptyFolder(val folderName: String) : FolderPlayerUiState

    data class Ready(
        val folderName: String,
        val entries: List<MediaEntry>,
        val displayOrder: List<Int>,
        val deactivatedOrder: List<Int> = emptyList(),
        val currentIndex: Int?,
        val selectedIndex: Int?,
        val status: PlayerStatus,
        val shuffleEnabled: Boolean = false,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        val isCurrentVideo: Boolean = false,
        val isFullscreen: Boolean = false,
    ) : FolderPlayerUiState
}
