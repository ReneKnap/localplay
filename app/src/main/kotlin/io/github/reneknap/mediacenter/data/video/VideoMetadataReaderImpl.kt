package io.github.reneknap.mediacenter.data.video

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoMetadataReaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : VideoMetadataReader {
        override suspend fun read(uri: String): VideoMetadata? =
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri.toUri())
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") {
                        return@withContext null
                    }
                    VideoMetadata(
                        durationMs = retriever.long(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: 0L,
                        width = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: 0,
                        height = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: 0,
                    )
                } catch (_: RuntimeException) {
                    // MediaMetadataRetriever throws IllegalArgument/IllegalState on broken or
                    // unreadable sources; treat any platform parse failure as "not a video".
                    null
                } finally {
                    retriever.release()
                }
            }

        private fun MediaMetadataRetriever.long(key: Int): Long? = extractMetadata(key)?.toLongOrNull()

        private fun MediaMetadataRetriever.int(key: Int): Int? = extractMetadata(key)?.toIntOrNull()
    }
