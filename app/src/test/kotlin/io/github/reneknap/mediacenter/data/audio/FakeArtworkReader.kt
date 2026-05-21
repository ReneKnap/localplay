package io.github.reneknap.mediacenter.data.audio

import android.graphics.Bitmap

class FakeArtworkReader(
    private val artwork: (String) -> Bitmap? = { null },
) : ArtworkReader {
    val requestedUris: MutableList<String> = mutableListOf()

    override suspend fun loadArtwork(uri: String): Bitmap? {
        requestedUris.add(uri)
        return artwork(uri)
    }
}
