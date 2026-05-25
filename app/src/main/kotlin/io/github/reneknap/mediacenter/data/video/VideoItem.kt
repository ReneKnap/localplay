package io.github.reneknap.mediacenter.data.video

data class VideoItem(
    val uri: String,
    val folderUri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val externalSubtitles: List<ExternalSubtitle> = emptyList(),
)
