package io.github.reneknap.mediacenter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.data.media.FolderMediaContent
import io.github.reneknap.mediacenter.data.media.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val folderRepository: FolderRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            mediaRepository.folders
                .map(::buildState)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = HomeUiState.Loading,
                )

        fun addFolder(uri: String) {
            viewModelScope.launch {
                folderRepository.addFolder(uri)
            }
        }

        fun removeFolder(uri: String) {
            viewModelScope.launch {
                folderRepository.removeFolder(uri)
            }
        }

        private fun buildState(folders: List<FolderMediaContent>): HomeUiState {
            if (folders.isEmpty()) return HomeUiState.Empty
            return HomeUiState.Folders(folders.map { FolderMediaUi(it.folder, it.scan) })
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
