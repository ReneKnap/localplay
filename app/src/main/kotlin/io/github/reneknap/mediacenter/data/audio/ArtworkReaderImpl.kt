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
import java.io.ByteArrayOutputStream
import java.util.Collections
import javax.inject.Inject

class ArtworkReaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArtworkReader {
        // Count-based eviction is fine: each entry is downsampled to at most its target size. Keyed by
        // "uri@size" so the transport-bar cover and the smaller row thumbnails coexist per track.
        private val cache = LruCache<String, Bitmap>(MAX_CACHED_COVERS)
        private val withoutArtwork = Collections.synchronizedSet(mutableSetOf<String>())

        // Compressed poster bytes for the system media controls, separate from the bitmap cache above.
        private val frameBytesCache = LruCache<String, ByteArray>(MAX_CACHED_POSTERS)
        private val withoutFrame = Collections.synchronizedSet(mutableSetOf<String>())

        override suspend fun loadArtwork(
            uri: String,
            targetSizePx: Int,
        ): Bitmap? {
            val cacheKey = "$uri@$targetSizePx"
            cache.get(cacheKey)?.let { return it }
            if (uri in withoutArtwork) return null
            return withContext(Dispatchers.IO) {
                val embedded = readEmbeddedPicture(uri)?.let { decodeDownsampled(it, targetSizePx) }
                // Fall back to a representative video frame: getFrameAtTime returns null for audio, so a
                // single uri-based path serves both kinds without the caller knowing the media kind.
                val bitmap = embedded ?: readVideoFrame(uri, targetSizePx)
                if (bitmap == null) {
                    withoutArtwork.add(uri)
                } else {
                    cache.put(cacheKey, bitmap)
                }
                bitmap
            }
        }

        override suspend fun loadVideoFrameBytes(
            uri: String,
            targetSizePx: Int,
        ): ByteArray? {
            val cacheKey = "$uri@$targetSizePx"
            frameBytesCache.get(cacheKey)?.let { return it }
            if (uri in withoutFrame) return null
            return withContext(Dispatchers.IO) {
                // Frame-only on purpose: audio (and frameless sources) yield null, so callers never
                // overwrite an audio track's embedded cover with this path.
                val frame = readVideoFrame(uri, targetSizePx)
                if (frame == null) {
                    withoutFrame.add(uri)
                    return@withContext null
                }
                val bytes =
                    ByteArrayOutputStream().use { stream ->
                        frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                        stream.toByteArray()
                    }
                frame.recycle()
                frameBytesCache.put(cacheKey, bytes)
                bytes
            }
        }

        private fun readVideoFrame(
            uri: String,
            targetSizePx: Int,
        ): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri.toUri())
                val frame = retriever.frameAtTime ?: return null
                scaleToTarget(frame, targetSizePx)
            } catch (_: RuntimeException) {
                null
            } finally {
                retriever.release()
            }
        }

        private fun scaleToTarget(
            bitmap: Bitmap,
            targetSizePx: Int,
        ): Bitmap {
            val minSide = minOf(bitmap.width, bitmap.height)
            if (minSide <= 0 || minSide <= targetSizePx) return bitmap
            val scale = targetSizePx.toFloat() / minSide
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            if (scaled != bitmap) bitmap.recycle()
            return scaled
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

        private fun decodeDownsampled(
            bytes: ByteArray,
            targetSizePx: Int,
        ): Bitmap? {
            val bounds =
                BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetSizePx)
                }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }

        private fun sampleSizeFor(
            width: Int,
            height: Int,
            targetSizePx: Int,
        ): Int {
            if (width <= 0 || height <= 0) return 1
            var inSampleSize = 1
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / inSampleSize >= targetSizePx && halfHeight / inSampleSize >= targetSizePx) {
                inSampleSize *= 2
            }
            return inSampleSize
        }

        private companion object {
            // Holds covers at multiple sizes per track (transport bar + row thumbnails).
            const val MAX_CACHED_COVERS = 64

            // Posters are larger (512px JPEG) and only the current/neighbouring items need one.
            const val MAX_CACHED_POSTERS = 16
            const val JPEG_QUALITY = 90
        }
    }
