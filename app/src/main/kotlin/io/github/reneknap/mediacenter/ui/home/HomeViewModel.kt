package io.github.reneknap.mediacenter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.audio.AudioRepository
import io.github.reneknap.mediacenter.data.folder.FolderRepository
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
        private val audioRepository: AudioRepository,
        private val folderRepository: FolderRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            audioRepository.folders
                .map { folders ->
                    if (folders.isEmpty()) HomeUiState.Empty else HomeUiState.Folders(folders)
                }
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

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
