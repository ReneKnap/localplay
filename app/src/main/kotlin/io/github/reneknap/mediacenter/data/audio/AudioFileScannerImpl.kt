package io.github.reneknap.mediacenter.data.audio

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AudioFileScannerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AudioFileScanner {
        override suspend fun scan(folderUri: String): List<RawAudioFile> =
            withContext(Dispatchers.IO) {
                val root = DocumentFile.fromTreeUri(context, folderUri.toUri()) ?: return@withContext emptyList()
                val results = mutableListOf<RawAudioFile>()
                walk(root, results)
                results
            }

        private fun walk(
            directory: DocumentFile,
            out: MutableList<RawAudioFile>,
        ) {
            for (child in directory.listFiles()) {
                if (child.isDirectory) {
                    walk(child, out)
                    continue
                }
                val name = child.name ?: continue
                if (!hasSupportedExtension(name)) continue
                out +=
                    RawAudioFile(
                        uri = child.uri.toString(),
                        displayName = name,
                        mimeType = child.type,
                        sizeBytes = child.length(),
                    )
            }
        }

        private fun hasSupportedExtension(fileName: String): Boolean {
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex < 0 || dotIndex == fileName.lastIndex) return false
            return fileName.substring(dotIndex + 1).lowercase() in SUPPORTED_AUDIO_EXTENSIONS
        }
    }
