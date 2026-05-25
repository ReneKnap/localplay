package io.github.reneknap.mediacenter.data.media

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaFileScannerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaFileScanner {
        override suspend fun scan(folderUri: String): MediaScanResult =
            withContext(Dispatchers.IO) {
                val root =
                    DocumentFile.fromTreeUri(context, folderUri.toUri())
                        ?: return@withContext MediaScanResult(emptyList(), emptyList())
                val media = mutableListOf<RawMediaFile>()
                val subtitles = mutableListOf<RawSubtitleFile>()
                walk(root, media, subtitles)
                MediaScanResult(media, subtitles)
            }

        private fun walk(
            directory: DocumentFile,
            media: MutableList<RawMediaFile>,
            subtitles: MutableList<RawSubtitleFile>,
        ) {
            val parentKey = directory.uri.toString()
            for (child in directory.listFiles()) {
                if (child.isDirectory) {
                    walk(child, media, subtitles)
                    continue
                }
                val name = child.name ?: continue
                val kind = mediaKindFor(name)
                when {
                    kind != null ->
                        media +=
                            RawMediaFile(
                                uri = child.uri.toString(),
                                displayName = name,
                                mimeType = child.type,
                                sizeBytes = child.length(),
                                kind = kind,
                                parentKey = parentKey,
                            )
                    isSubtitleFile(name) ->
                        subtitles +=
                            RawSubtitleFile(
                                uri = child.uri.toString(),
                                displayName = name,
                                parentKey = parentKey,
                            )
                }
            }
        }
    }
