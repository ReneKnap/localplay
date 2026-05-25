package io.github.reneknap.mediacenter.data.media

data class RawMediaFile(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val kind: MediaKind,
    // The containing directory's identity, so subtitle sidecars can be matched to siblings (ADR-011).
    val parentKey: String,
)
