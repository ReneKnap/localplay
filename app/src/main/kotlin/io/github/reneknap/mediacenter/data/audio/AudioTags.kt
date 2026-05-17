package io.github.reneknap.mediacenter.data.audio

data class AudioTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
) {
    companion object {
        val EMPTY = AudioTags(title = null, artist = null, album = null, durationMs = null)
    }
}
