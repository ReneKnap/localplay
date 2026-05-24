package io.github.reneknap.mediacenter.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
