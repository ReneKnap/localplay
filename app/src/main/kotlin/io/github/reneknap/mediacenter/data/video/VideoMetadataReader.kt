package io.github.reneknap.mediacenter.data.video

interface VideoMetadataReader {
    /**
     * Reads duration and resolution for [uri], or returns `null` when the source carries no video
     * track (audio-only containers such as some `.3gp`/`.mp4` files).
     */
    suspend fun read(uri: String): VideoMetadata?
}
