package io.github.reneknap.mediacenter.data.audio

import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl
    @Inject
    constructor(
        private val folderRepository: FolderRepository,
        private val scanner: AudioFileScanner,
        private val tagReader: TagReader,
        @ApplicationScope private val scope: CoroutineScope,
    ) : AudioRepository {
        private val cache = MutableStateFlow<Map<String, FolderScanState>>(emptyMap())
        private val inFlight = mutableSetOf<String>()
        private val mutex = Mutex()

        init {
            scope.launch {
                folderRepository.folders.collect { entries -> syncScans(entries) }
            }
        }

        override val folders: Flow<List<FolderTracks>> =
            combine(
                folderRepository.folders,
                cache,
            ) { entries, cacheMap ->
                entries.map { entry ->
                    FolderTracks(entry, cacheMap[entry.uri] ?: FolderScanState.Scanning)
                }
            }.distinctUntilChanged()

        private suspend fun syncScans(entries: List<FolderEntry>) {
            mutex.withLock {
                val incomingUris = entries.map { it.uri }.toSet()
                cache.update { current -> current.filterKeys { it in incomingUris } }

                for (entry in entries) {
                    if (shouldSkip(entry)) continue
                    if (!entry.isReachable) {
                        cache.update { it + (entry.uri to FolderScanState.Unreachable) }
                        continue
                    }
                    inFlight += entry.uri
                    cache.update { it + (entry.uri to FolderScanState.Scanning) }
                    scope.launch { runScan(entry.uri) }
                }
            }
        }

        private fun shouldSkip(entry: FolderEntry): Boolean {
            if (entry.uri in inFlight) return true
            return cache.value[entry.uri] is FolderScanState.Ready
        }

        private suspend fun runScan(folderUri: String) {
            try {
                val tracks =
                    scanner.scan(folderUri)
                        .map { file -> toTrack(file, folderUri) }
                        .sortedBy { it.displayName.lowercase() }
                cache.update { it + (folderUri to FolderScanState.Ready(tracks)) }
            } catch (_: Exception) {
                // Defensive: any platform failure (SecurityException mid-scan, OOM on huge tag,
                // unexpected DocumentsContract exception) falls back to Unreachable so the UI
                // does not stay stuck on Scanning forever.
                cache.update { it + (folderUri to FolderScanState.Unreachable) }
            } finally {
                mutex.withLock { inFlight -= folderUri }
            }
        }

        private suspend fun toTrack(
            file: RawAudioFile,
            folderUri: String,
        ): AudioTrack {
            val tags = tagReader.readTags(file.uri)
            return AudioTrack(
                uri = file.uri,
                folderUri = folderUri,
                displayName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                title = tags.title ?: file.displayName.substringBeforeLast('.'),
                artist = tags.artist,
                album = tags.album,
                durationMs = tags.durationMs ?: 0L,
            )
        }
    }
