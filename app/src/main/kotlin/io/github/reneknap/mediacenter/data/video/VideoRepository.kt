package io.github.reneknap.mediacenter.data.video

import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    val folders: Flow<List<FolderVideos>>
}
