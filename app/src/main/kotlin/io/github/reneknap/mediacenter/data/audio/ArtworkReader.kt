package io.github.reneknap.mediacenter.data.audio

import android.graphics.Bitmap

interface ArtworkReader {
    suspend fun loadArtwork(
        uri: String,
        targetSizePx: Int = DEFAULT_TARGET_SIZE_PX,
    ): Bitmap?

    /**
     * A representative video frame of [uri], compressed to bytes for use as system media artwork
     * (Media3 `MediaMetadata.artworkData`). Returns null for audio or any source without a decodable
     * frame, so a non-null result doubles as a "this is a playable video frame" signal.
     */
    suspend fun loadVideoFrameBytes(
        uri: String,
        targetSizePx: Int = POSTER_SIZE_PX,
    ): ByteArray?

    companion object {
        const val DEFAULT_TARGET_SIZE_PX = 256

        // Larger than the in-app cover so the lockscreen poster stays sharp on big displays.
        const val POSTER_SIZE_PX = 512
    }
}
