package io.github.reneknap.mediacenter.data.media

import androidx.media3.common.MimeTypes

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

// Subtitle sidecar whitelist per ADR-011. Subtitles are NOT a MediaKind — they enrich a video and
// must never be listed as playable media. Kept here as the single source of truth for the walk.
val SUPPORTED_SUBTITLE_EXTENSIONS: Set<String> =
    setOf(
        "srt",
        "vtt",
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

/** True when [fileName] has a supported subtitle extension (`.srt` / `.vtt`), case-insensitive. */
fun isSubtitleFile(fileName: String): Boolean {
    val ext = extensionOf(fileName) ?: return false
    return ext in SUPPORTED_SUBTITLE_EXTENSIONS
}

/**
 * The Media3 MIME type for a subtitle file (`application/x-subrip` for `.srt`, `text/vtt` for
 * `.vtt`), or `null` when [fileName] is not a supported subtitle.
 */
fun subtitleMimeFor(fileName: String): String? =
    when (extensionOf(fileName)) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "vtt" -> MimeTypes.TEXT_VTT
        else -> null
    }

/** The lowercase extension of [fileName], or `null` when it has no usable extension. */
private fun extensionOf(fileName: String): String? {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == fileName.lastIndex) return null
    return fileName.substring(dotIndex + 1).lowercase()
}
