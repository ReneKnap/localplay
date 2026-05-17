package io.github.reneknap.mediacenter.data.audio

data class RawAudioFile(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
)
