package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.CompletableDeferred

class FakeMediaFileScanner : MediaFileScanner {
    private val pending = mutableMapOf<String, CompletableDeferred<MediaScanResult>>()
    val scannedUris: MutableList<String> = mutableListOf()

    fun setResult(
        folderUri: String,
        media: List<RawMediaFile>,
        subtitles: List<RawSubtitleFile> = emptyList(),
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.complete(MediaScanResult(media, subtitles))
    }

    fun failWith(
        folderUri: String,
        error: Throwable,
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.completeExceptionally(error)
    }

    override suspend fun scan(folderUri: String): MediaScanResult {
        scannedUris.add(folderUri)
        return pending.getOrPut(folderUri) { CompletableDeferred() }.await()
    }
}
