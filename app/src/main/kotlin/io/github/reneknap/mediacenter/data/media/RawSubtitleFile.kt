package io.github.reneknap.mediacenter.data.media

/**
 * A subtitle sidecar file found during the folder walk (ADR-011). [parentKey] is the containing
 * directory's identity so matching can be scoped to siblings of the video. The MIME type is derived
 * from the extension (see [subtitleMimeFor]) rather than the unreliable SAF `DocumentFile.type`.
 */
data class RawSubtitleFile(
    val uri: String,
    val displayName: String,
    val parentKey: String,
)
