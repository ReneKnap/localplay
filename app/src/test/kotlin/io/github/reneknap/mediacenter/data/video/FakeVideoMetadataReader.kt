package io.github.reneknap.mediacenter.data.video

class FakeVideoMetadataReader : VideoMetadataReader {
    private val metadata = mutableMapOf<String, VideoMetadata?>()
    val readUris: MutableList<String> = mutableListOf()

    fun setMetadata(
        uri: String,
        metadata: VideoMetadata?,
    ) {
        this.metadata[uri] = metadata
    }

    override suspend fun read(uri: String): VideoMetadata? {
        readUris.add(uri)
        // Default to null (no video track) when unset — mirrors an audio-only container.
        return metadata[uri]
    }
}
