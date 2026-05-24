package io.github.reneknap.mediacenter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.audio.AudioRepository
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.data.video.FolderVideos
import io.github.reneknap.mediacenter.data.video.VideoRepository
import io.github.reneknap.mediacenter.data.video.VideoScanState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val audioRepository: AudioRepository,
        private val videoRepository: VideoRepository,
        private val folderRepository: FolderRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            combine(
                audioRepository.folders,
                videoRepository.folders,
            ) { audioFolders, videoFolders ->
                buildState(audioFolders, videoFolders)
            }.stateIn(
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

        private fun buildState(
            audioFolders: List<FolderTracks>,
            videoFolders: List<FolderVideos>,
        ): HomeUiState {
            if (audioFolders.isEmpty()) return HomeUiState.Empty

            val videoByUri = videoFolders.associateBy { it.folder.uri }
            val items =
                audioFolders.map { audioFolder ->
                    FolderMediaUi(
                        folder = audioFolder.folder,
                        audio = audioFolder.scan,
                        video = videoByUri[audioFolder.folder.uri]?.scan ?: VideoScanState.Scanning,
                    )
                }
            return HomeUiState.Folders(items)
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
