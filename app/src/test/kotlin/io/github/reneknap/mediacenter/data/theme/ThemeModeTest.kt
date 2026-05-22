package io.github.reneknap.mediacenter.data.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `next from DARK is LIGHT`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.DARK.next())
    }

    @Test
    fun `next from LIGHT is SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.LIGHT.next())
    }

    @Test
    fun `next from SYSTEM wraps to DARK`() {
        assertEquals(ThemeMode.DARK, ThemeMode.SYSTEM.next())
    }
}
