package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.flow.Flow

/**
 * Combined projection over [MediaScanIndex] (ADR-010): each folder exposes one ordered list of
 * [MediaEntry] mixing audio and video. Replaces the separate AudioRepository/VideoRepository.
 */
interface MediaRepository {
    val folders: Flow<List<FolderMediaContent>>
}
