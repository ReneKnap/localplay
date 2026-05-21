package io.github.reneknap.mediacenter.ui.player

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.playback.PlayerStatus

sealed interface FolderPlayerUiState {
    data object Loading : FolderPlayerUiState

    data object NotAvailable : FolderPlayerUiState

    data class Ready(
        val folderName: String,
        val tracks: List<AudioTrack>,
        val displayOrder: List<Int>,
        val deactivatedOrder: List<Int> = emptyList(),
        val currentIndex: Int?,
        val selectedIndex: Int?,
        val status: PlayerStatus,
        val shuffleEnabled: Boolean = false,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
    ) : FolderPlayerUiState
}
