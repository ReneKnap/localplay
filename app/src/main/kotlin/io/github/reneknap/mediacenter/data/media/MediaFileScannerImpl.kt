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
        override suspend fun scan(folderUri: String): List<RawMediaFile> =
            withContext(Dispatchers.IO) {
                val root = DocumentFile.fromTreeUri(context, folderUri.toUri()) ?: return@withContext emptyList()
                val results = mutableListOf<RawMediaFile>()
                walk(root, results)
                results
            }

        private fun walk(
            directory: DocumentFile,
            out: MutableList<RawMediaFile>,
        ) {
            for (child in directory.listFiles()) {
                if (child.isDirectory) {
                    walk(child, out)
                    continue
                }
                val name = child.name ?: continue
                val kind = mediaKindFor(name) ?: continue
                out +=
                    RawMediaFile(
                        uri = child.uri.toString(),
                        displayName = name,
                        mimeType = child.type,
                        sizeBytes = child.length(),
                        kind = kind,
                    )
            }
        }
    }
