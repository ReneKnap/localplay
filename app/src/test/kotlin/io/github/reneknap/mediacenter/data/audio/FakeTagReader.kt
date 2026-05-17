package io.github.reneknap.mediacenter.data.audio

class FakeTagReader : TagReader {
    private val tags = mutableMapOf<String, AudioTags>()
    val readUris: MutableList<String> = mutableListOf()

    fun setTags(
        uri: String,
        tags: AudioTags,
    ) {
        this.tags[uri] = tags
    }

    override suspend fun readTags(uri: String): AudioTags {
        readUris.add(uri)
        return tags[uri] ?: AudioTags.EMPTY
    }
}
