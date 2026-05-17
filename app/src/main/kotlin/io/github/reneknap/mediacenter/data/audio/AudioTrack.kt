package io.github.reneknap.mediacenter.data.audio

data class AudioTrack(
    val uri: String,
    val folderUri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
)
