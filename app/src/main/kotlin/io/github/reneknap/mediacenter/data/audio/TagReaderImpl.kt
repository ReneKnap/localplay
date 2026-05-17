package io.github.reneknap.mediacenter.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TagReaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TagReader {
        override suspend fun readTags(uri: String): AudioTags =
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri.toUri())
                    AudioTags(
                        title = retriever.string(MediaMetadataRetriever.METADATA_KEY_TITLE),
                        artist = retriever.string(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                        album = retriever.string(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                        durationMs =
                            retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull(),
                    )
                } catch (_: RuntimeException) {
                    // MediaMetadataRetriever throws IllegalArgument/IllegalState on broken or
                    // unreadable sources; treat any platform parse failure as "no tags available".
                    AudioTags.EMPTY
                } finally {
                    retriever.release()
                }
            }

        private fun MediaMetadataRetriever.string(key: Int): String? = extractMetadata(key)?.takeIf { it.isNotBlank() }
    }
