package io.github.reneknap.mediacenter.data.audio

interface TagReader {
    suspend fun readTags(uri: String): AudioTags
}
