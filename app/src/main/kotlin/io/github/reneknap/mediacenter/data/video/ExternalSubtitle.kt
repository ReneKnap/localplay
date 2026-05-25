package io.github.reneknap.mediacenter.data.video

/**
 * An external subtitle sidecar file matched to a video (ADR-011). [mimeType] is a Media3 MIME
 * (`application/x-subrip` / `text/vtt`); [language] is the BCP-47-ish token parsed from a name
 * suffix (e.g. `film.de.srt` -> `de`), or null when the name carries no language segment.
 */
data class ExternalSubtitle(
    val uri: String,
    val mimeType: String,
    val language: String?,
    val label: String,
)
