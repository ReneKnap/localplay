package io.github.reneknap.mediacenter.data.media

/**
 * The output of one folder walk (ADR-008 single walk): playable [media] plus the subtitle sidecar
 * files found alongside them ([subtitles]), kept apart so subtitles are never treated as media.
 */
data class MediaScanResult(
    val media: List<RawMediaFile>,
    val subtitles: List<RawSubtitleFile>,
)
