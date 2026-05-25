package io.github.reneknap.mediacenter.data.media

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedMediaTest {
    @Test
    fun `audio extensions classify as AUDIO`() {
        SUPPORTED_AUDIO_EXTENSIONS.forEach { ext ->
            assertEquals("$ext should be AUDIO", MediaKind.AUDIO, mediaKindFor("clip.$ext"))
        }
    }

    @Test
    fun `video extensions classify as VIDEO`() {
        SUPPORTED_VIDEO_EXTENSIONS.forEach { ext ->
            assertEquals("$ext should be VIDEO", MediaKind.VIDEO, mediaKindFor("clip.$ext"))
        }
    }

    @Test
    fun `unknown extensions classify as null`() {
        assertNull(mediaKindFor("notes.txt"))
        assertNull(mediaKindFor("cover.jpg"))
        assertNull(mediaKindFor("archive.zip"))
    }

    @Test
    fun `classification is case-insensitive`() {
        assertEquals(MediaKind.VIDEO, mediaKindFor("Movie.MP4"))
        assertEquals(MediaKind.VIDEO, mediaKindFor("Movie.Mkv"))
        assertEquals(MediaKind.AUDIO, mediaKindFor("Song.FLAC"))
    }

    @Test
    fun `file without extension classifies as null`() {
        assertNull(mediaKindFor("README"))
        assertNull(mediaKindFor("noextension"))
    }

    @Test
    fun `trailing dot classifies as null`() {
        assertNull(mediaKindFor("weird."))
    }

    @Test
    fun `dotfile without real extension classifies as null`() {
        assertNull(mediaKindFor(".gitignore"))
    }

    // --- Subtitles (ADR-011) ---------------------------------------------

    @Test
    fun `srt and vtt are recognized as subtitle files`() {
        assertTrue(isSubtitleFile("film.srt"))
        assertTrue(isSubtitleFile("film.vtt"))
    }

    @Test
    fun `subtitle recognition is case-insensitive`() {
        assertTrue(isSubtitleFile("Film.SRT"))
        assertTrue(isSubtitleFile("Film.Vtt"))
    }

    @Test
    fun `non-subtitle files are not recognized as subtitles`() {
        assertFalse(isSubtitleFile("film.mp4"))
        assertFalse(isSubtitleFile("song.mp3"))
        assertFalse(isSubtitleFile("notes.txt"))
        assertFalse(isSubtitleFile("noextension"))
    }

    @Test
    fun `subtitle MIME maps srt to subrip and vtt to webvtt`() {
        assertEquals(MimeTypes.APPLICATION_SUBRIP, subtitleMimeFor("film.srt"))
        assertEquals(MimeTypes.TEXT_VTT, subtitleMimeFor("film.vtt"))
        assertEquals(MimeTypes.APPLICATION_SUBRIP, subtitleMimeFor("Film.SRT"))
    }

    @Test
    fun `subtitle MIME is null for non-subtitle files`() {
        assertNull(subtitleMimeFor("film.mp4"))
        assertNull(subtitleMimeFor("notes.txt"))
    }

    @Test
    fun `subtitle extensions are never classified as playable media`() {
        SUPPORTED_SUBTITLE_EXTENSIONS.forEach { ext ->
            assertNull("$ext must not be AUDIO/VIDEO", mediaKindFor("film.$ext"))
        }
    }
}
