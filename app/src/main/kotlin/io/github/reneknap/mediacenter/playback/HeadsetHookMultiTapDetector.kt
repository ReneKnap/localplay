package io.github.reneknap.mediacenter.playback

/**
 * Counts rapid taps on a single-button headset's hook so a burst can be resolved into a
 * transport action: 1 = play/pause, 2 = next, 3 = previous.
 *
 * Pure and framework-free: callers feed monotonic tap timestamps (e.g. `KeyEvent.eventTime`)
 * and call [resolveBurst] once the quiet window has elapsed. Two taps no more than
 * [multiTapWindowMs] apart belong to the same burst.
 */
class HeadsetHookMultiTapDetector(
    private val multiTapWindowMs: Long = DEFAULT_WINDOW_MS,
) {
    private var burstTapCount = 0
    private var lastTapUptimeMs = 0L

    /** Registers a tap at [uptimeMs] and returns the running 1-based count of the current burst. */
    fun registerTap(uptimeMs: Long): Int {
        val continuesBurst = burstTapCount > 0 && uptimeMs - lastTapUptimeMs <= multiTapWindowMs
        burstTapCount = if (continuesBurst) burstTapCount + 1 else 1
        lastTapUptimeMs = uptimeMs
        return burstTapCount
    }

    /** Resolves the accumulated burst into an action and resets the count for the next burst. */
    fun resolveBurst(): MediaButtonAction? {
        val action = MediaButtonAction.forTapCount(burstTapCount)
        burstTapCount = 0
        return action
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 300L
    }
}
