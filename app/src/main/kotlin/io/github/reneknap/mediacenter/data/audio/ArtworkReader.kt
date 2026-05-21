package io.github.reneknap.mediacenter.data.audio

import android.graphics.Bitmap

interface ArtworkReader {
    suspend fun loadArtwork(
        uri: String,
        targetSizePx: Int = DEFAULT_TARGET_SIZE_PX,
    ): Bitmap?

    companion object {
        const val DEFAULT_TARGET_SIZE_PX = 256
    }
}
