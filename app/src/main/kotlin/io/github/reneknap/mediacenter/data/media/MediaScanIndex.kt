package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.flow.Flow

interface MediaScanIndex {
    val folders: Flow<List<FolderMedia>>
}
