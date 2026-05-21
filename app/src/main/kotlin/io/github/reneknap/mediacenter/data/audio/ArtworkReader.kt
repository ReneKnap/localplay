package io.github.reneknap.mediacenter.data.audio

import android.graphics.Bitmap

interface ArtworkReader {
    suspend fun loadArtwork(uri: String): Bitmap?
}
