package io.github.reneknap.mediacenter.data.audio

import kotlinx.coroutines.CompletableDeferred

class FakeAudioFileScanner : AudioFileScanner {
    private val pending = mutableMapOf<String, CompletableDeferred<List<RawAudioFile>>>()
    val scannedUris: MutableList<String> = mutableListOf()

    fun setResult(
        folderUri: String,
        files: List<RawAudioFile>,
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.complete(files)
    }

    fun failWith(
        folderUri: String,
        error: Throwable,
    ) {
        pending.getOrPut(folderUri) { CompletableDeferred() }.completeExceptionally(error)
    }

    override suspend fun scan(folderUri: String): List<RawAudioFile> {
        scannedUris.add(folderUri)
        return pending.getOrPut(folderUri) { CompletableDeferred() }.await()
    }
}
