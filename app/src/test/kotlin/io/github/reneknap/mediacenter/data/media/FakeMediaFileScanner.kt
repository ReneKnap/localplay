package io.github.reneknap.mediacenter.data.media

import kotlinx.coroutines.CompletableDeferred

class FakeMediaFileScanner : MediaFileScanner {
    private val pending = mutableMapOf<String, CompletableDeferred<List<RawMediaFile>>>()
    val scannedUris: MutableList<String> = mutableListOf()

    fun setResult(
        folderUri: String,
        files: List<RawMediaFile>,
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.complete(files)
    }

    fun failWith(
        folderUri: String,
        error: Throwable,
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.completeExceptionally(error)
    }

    override suspend fun scan(folderUri: String): List<RawMediaFile> {
        scannedUris.add(folderUri)
        return pending.getOrPut(folderUri) { CompletableDeferred() }.await()
    }
}
