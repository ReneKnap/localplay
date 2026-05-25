package io.github.reneknap.mediacenter.data.media

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleMatchingTest {
    private val dir = "content://media/folder"

    private fun sub(
        name: String,
        parentKey: String = dir,
    ) = RawSubtitleFile(uri = "$parentKey/$name", displayName = name, parentKey = parentKey)

    @Test
    fun `exact basename match attaches the subtitle with no language`() {
        val result = matchSubtitles("film.mp4", dir, listOf(sub("film.srt")))

        assertEquals(1, result.size)
        assertEquals("$dir/film.srt", result[0].uri)
        assertNull(result[0].language)
    }

    @Test
    fun `language-suffixed sidecars match and parse the language`() {
        val result =
            matchSubtitles(
                "film.mp4",
                dir,
                listOf(sub("film.de.srt"), sub("film.en.vtt")),
            )

        assertEquals(2, result.size)
        val byLang = result.associateBy { it.language }
        assertTrue("expected a German track", byLang.containsKey("de"))
        assertTrue("expected an English track", byLang.containsKey("en"))
        assertEquals("$dir/film.de.srt", byLang["de"]!!.uri)
        assertEquals("$dir/film.en.vtt", byLang["en"]!!.uri)
    }

    @Test
    fun `subtitle MIME is derived from the extension`() {
        val result =
            matchSubtitles(
                "film.mp4",
                dir,
                listOf(sub("film.srt"), sub("film.en.vtt")),
            )

        val srt = result.first { it.uri.endsWith(".srt") }
        val vtt = result.first { it.uri.endsWith(".vtt") }
        assertEquals(MimeTypes.APPLICATION_SUBRIP, srt.mimeType)
        assertEquals(MimeTypes.TEXT_VTT, vtt.mimeType)
    }

    @Test
    fun `a different basename is not matched`() {
        val result = matchSubtitles("film.mp4", dir, listOf(sub("other.srt")))

        assertTrue("unrelated sidecar must not match", result.isEmpty())
    }

    @Test
    fun `a basename that is only a prefix is not matched`() {
        // film2 starts with "film" but is a different title — the trailing-dot rule must exclude it.
        val result = matchSubtitles("film.mp4", dir, listOf(sub("film2.srt")))

        assertTrue("prefix-only name must not match", result.isEmpty())
    }

    @Test
    fun `a matching name in a different directory is not matched`() {
        val result =
            matchSubtitles(
                "film.mp4",
                dir,
                listOf(sub("film.srt", parentKey = "content://media/other")),
            )

        assertTrue("sidecar from another directory must not match", result.isEmpty())
    }

    @Test
    fun `subtitle extension matching is case-insensitive`() {
        val result = matchSubtitles("film.mp4", dir, listOf(sub("film.SRT")))

        assertEquals(1, result.size)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, result[0].mimeType)
    }

    @Test
    fun `every matched subtitle carries a non-blank label`() {
        val result =
            matchSubtitles(
                "film.mp4",
                dir,
                listOf(sub("film.srt"), sub("film.de.srt")),
            )

        assertEquals(2, result.size)
        result.forEach { assertTrue("label must not be blank", it.label.isNotBlank()) }
    }

    @Test
    fun `no sidecars yields no subtitles`() {
        assertTrue(matchSubtitles("film.mp4", dir, emptyList()).isEmpty())
    }
}
