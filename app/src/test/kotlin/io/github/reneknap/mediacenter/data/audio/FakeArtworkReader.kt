package io.github.reneknap.mediacenter.data.audio

import android.graphics.Bitmap

class FakeArtworkReader(
    private val artwork: (String) -> Bitmap? = { null },
    private val videoFrameBytes: (String) -> ByteArray? = { null },
) : ArtworkReader {
    val requestedUris: MutableList<String> = mutableListOf()
    val requestedFrameUris: MutableList<String> = mutableListOf()

    override suspend fun loadArtwork(
        uri: String,
        targetSizePx: Int,
    ): Bitmap? {
        requestedUris.add(uri)
        return artwork(uri)
    }

    override suspend fun loadVideoFrameBytes(
        uri: String,
        targetSizePx: Int,
    ): ByteArray? {
        requestedFrameUris.add(uri)
        return videoFrameBytes(uri)
    }
}
