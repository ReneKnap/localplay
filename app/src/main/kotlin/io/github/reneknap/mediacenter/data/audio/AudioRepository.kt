package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    val folders: Flow<List<FolderTracks>>
}
