package io.github.reneknap.mediacenter.data.media

// Audio whitelist per ADR-001, video whitelist per ADR-008. Centralized here as the single
// source of truth for the classifying folder walk (MediaFileScanner).
val SUPPORTED_AUDIO_EXTENSIONS: Set<String> =
    setOf(
        "mp3",
        "m4a",
        "aac",
        "flac",
        "ogg",
        "opus",
        "wav",
    )

val SUPPORTED_VIDEO_EXTENSIONS: Set<String> =
    setOf(
        "mp4",
        "m4v",
        "mov",
        "mkv",
        "webm",
        "3gp",
        "ts",
        "avi",
    )

/**
 * Classifies a file by its extension into a [MediaKind], or `null` when the file has no usable
 * extension or matches neither whitelist.
 */
fun mediaKindFor(fileName: String): MediaKind? {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == fileName.lastIndex) return null
    return when (fileName.substring(dotIndex + 1).lowercase()) {
        in SUPPORTED_AUDIO_EXTENSIONS -> MediaKind.AUDIO
        in SUPPORTED_VIDEO_EXTENSIONS -> MediaKind.VIDEO
        else -> null
    }
}
