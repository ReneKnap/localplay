package io.github.reneknap.mediacenter.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadsetHookMultiTapDetectorTest {
    // ---------------------------------------------------------------------
    // MediaButtonAction.forTapCount — pure mapping
    // ---------------------------------------------------------------------

    @Test
    fun `forTapCount 0 returns null`() {
        assertNull(MediaButtonAction.forTapCount(0))
    }

    @Test
    fun `forTapCount negative returns null`() {
        assertNull(MediaButtonAction.forTapCount(-1))
    }

    @Test
    fun `forTapCount 1 returns PLAY_PAUSE`() {
        assertEquals(MediaButtonAction.PLAY_PAUSE, MediaButtonAction.forTapCount(1))
    }

    @Test
    fun `forTapCount 2 returns NEXT`() {
        assertEquals(MediaButtonAction.NEXT, MediaButtonAction.forTapCount(2))
    }

    @Test
    fun `forTapCount 3 returns PREVIOUS`() {
        assertEquals(MediaButtonAction.PREVIOUS, MediaButtonAction.forTapCount(3))
    }

    @Test
    fun `forTapCount above 3 clamps to PREVIOUS`() {
        assertEquals(MediaButtonAction.PREVIOUS, MediaButtonAction.forTapCount(4))
        assertEquals(MediaButtonAction.PREVIOUS, MediaButtonAction.forTapCount(7))
    }

    // ---------------------------------------------------------------------
    // registerTap — burst counting within / across the window
    // ---------------------------------------------------------------------

    @Test
    fun `single tap reports burst count 1`() {
        val detector = HeadsetHookMultiTapDetector()

        assertEquals(1, detector.registerTap(uptimeMs = 0L))
    }

    @Test
    fun `taps within the window increment the burst count`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)

        assertEquals(1, detector.registerTap(uptimeMs = 0L))
        assertEquals(2, detector.registerTap(uptimeMs = 100L))
        assertEquals(3, detector.registerTap(uptimeMs = 250L))
    }

    @Test
    fun `tap at exactly the window boundary continues the burst`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)

        detector.registerTap(uptimeMs = 0L)

        assertEquals(2, detector.registerTap(uptimeMs = 300L))
    }

    @Test
    fun `tap beyond the window starts a new burst`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)

        detector.registerTap(uptimeMs = 0L)

        assertEquals(1, detector.registerTap(uptimeMs = 301L))
    }

    @Test
    fun `gap is measured from the previous tap, not the burst start`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)

        // Three taps each 200 ms apart: each gap is within the window, so the
        // burst keeps growing even though the total span exceeds the window.
        assertEquals(1, detector.registerTap(uptimeMs = 0L))
        assertEquals(2, detector.registerTap(uptimeMs = 200L))
        assertEquals(3, detector.registerTap(uptimeMs = 400L))
    }

    // ---------------------------------------------------------------------
    // resolveBurst — count → action and reset
    // ---------------------------------------------------------------------

    @Test
    fun `single tap resolves to PLAY_PAUSE`() {
        val detector = HeadsetHookMultiTapDetector()
        detector.registerTap(uptimeMs = 0L)

        assertEquals(MediaButtonAction.PLAY_PAUSE, detector.resolveBurst())
    }

    @Test
    fun `two taps within the window resolve to a single NEXT burst`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)
        detector.registerTap(uptimeMs = 0L)
        detector.registerTap(uptimeMs = 100L)

        assertEquals(MediaButtonAction.NEXT, detector.resolveBurst())
    }

    @Test
    fun `three taps within the window resolve to PREVIOUS`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)
        detector.registerTap(uptimeMs = 0L)
        detector.registerTap(uptimeMs = 100L)
        detector.registerTap(uptimeMs = 200L)

        assertEquals(MediaButtonAction.PREVIOUS, detector.resolveBurst())
    }

    @Test
    fun `resolveBurst on a fresh detector returns null`() {
        val detector = HeadsetHookMultiTapDetector()

        assertNull(detector.resolveBurst())
    }

    @Test
    fun `resolveBurst resets the count so a second call returns null`() {
        val detector = HeadsetHookMultiTapDetector()
        detector.registerTap(uptimeMs = 0L)
        detector.resolveBurst()

        assertNull(detector.resolveBurst())
    }

    @Test
    fun `two taps spaced beyond the window are two separate PLAY_PAUSE bursts`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)

        detector.registerTap(uptimeMs = 0L)
        assertEquals(MediaButtonAction.PLAY_PAUSE, detector.resolveBurst())

        detector.registerTap(uptimeMs = 1_000L)
        assertEquals(MediaButtonAction.PLAY_PAUSE, detector.resolveBurst())
    }

    @Test
    fun `a new burst after resolve starts counting from 1`() {
        val detector = HeadsetHookMultiTapDetector(multiTapWindowMs = 300L)
        detector.registerTap(uptimeMs = 0L)
        detector.registerTap(uptimeMs = 100L)
        detector.resolveBurst() // consumes the NEXT burst

        // A tap close in time to the previous one must still be a fresh burst,
        // because resolveBurst cleared the accumulated count.
        assertEquals(1, detector.registerTap(uptimeMs = 150L))
        assertEquals(MediaButtonAction.PLAY_PAUSE, detector.resolveBurst())
    }
}
