package io.github.reneknap.mediacenter.data.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

class ArtworkReaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArtworkReader {
        // Count-based eviction is fine: each entry is downsampled to at most TARGET_SIZE_PX.
        private val cache = LruCache<String, Bitmap>(MAX_CACHED_COVERS)
        private val withoutArtwork = Collections.synchronizedSet(mutableSetOf<String>())

        override suspend fun loadArtwork(uri: String): Bitmap? {
            cache.get(uri)?.let { return it }
            if (uri in withoutArtwork) return null
            return withContext(Dispatchers.IO) {
                val bytes = readEmbeddedPicture(uri)
                val bitmap = bytes?.let { decodeDownsampled(it) }
                if (bitmap == null) {
                    withoutArtwork.add(uri)
                } else {
                    cache.put(uri, bitmap)
                }
                bitmap
            }
        }

        private fun readEmbeddedPicture(uri: String): ByteArray? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri.toUri())
                retriever.embeddedPicture
            } catch (_: RuntimeException) {
                // MediaMetadataRetriever throws IllegalArgument/IllegalState on broken or
                // unreadable sources; treat any platform failure as "no artwork".
                null
            } finally {
                retriever.release()
            }
        }

        private fun decodeDownsampled(bytes: ByteArray): Bitmap? {
            val bounds =
                BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }

        private fun sampleSizeFor(
            width: Int,
            height: Int,
        ): Int {
            if (width <= 0 || height <= 0) return 1
            var inSampleSize = 1
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / inSampleSize >= TARGET_SIZE_PX && halfHeight / inSampleSize >= TARGET_SIZE_PX) {
                inSampleSize *= 2
            }
            return inSampleSize
        }

        private companion object {
            const val MAX_CACHED_COVERS = 32
            const val TARGET_SIZE_PX = 256
        }
    }
