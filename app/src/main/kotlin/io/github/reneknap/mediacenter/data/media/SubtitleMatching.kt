package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.video.ExternalSubtitle

/**
 * Matches subtitle sidecar files to a video by basename within the same directory, deriving the
 * language from a name suffix (`film.de.srt` -> `de`). Pure, no I/O (ADR-011).
 *
 * Match rule: a subtitle matches when it lives in [videoParentKey] and its name (minus the subtitle
 * extension) either equals the video basename or begins with "<videoBasename>." — the trailing dot
 * prevents `film2.srt` from matching `film.mp4`. The segment after "<videoBasename>." becomes the
 * language; an exact basename match has no language.
 */
fun matchSubtitles(
    videoName: String,
    videoParentKey: String,
    subtitles: List<RawSubtitleFile>,
): List<ExternalSubtitle> {
    val videoBase = videoName.substringBeforeLast('.')
    return subtitles.mapNotNull { subtitle ->
        if (subtitle.parentKey != videoParentKey) return@mapNotNull null
        val mimeType = subtitleMimeFor(subtitle.displayName) ?: return@mapNotNull null
        val subtitleBase = subtitle.displayName.substringBeforeLast('.')
        val language =
            when {
                subtitleBase == videoBase -> null
                subtitleBase.startsWith("$videoBase.") -> subtitleBase.substringAfter("$videoBase.")
                else -> return@mapNotNull null
            }
        ExternalSubtitle(
            uri = subtitle.uri,
            mimeType = mimeType,
            language = language,
            label = language ?: subtitleBase,
        )
    }
}
