package io.github.reneknap.mediacenter.playback

/** Transport action derived from a burst of single-button headset-hook taps. */
enum class MediaButtonAction {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    ;

    companion object {
        /**
         * Maps a 1-based tap [count] to its transport action: 1 tap toggles play/pause,
         * 2 skips to next, 3 or more skips to previous. A non-positive count yields no action.
         */
        fun forTapCount(count: Int): MediaButtonAction? =
            when {
                count <= 0 -> null
                count == 1 -> PLAY_PAUSE
                count == 2 -> NEXT
                else -> PREVIOUS
            }
    }
}
