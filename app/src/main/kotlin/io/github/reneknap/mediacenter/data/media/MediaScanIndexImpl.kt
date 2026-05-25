package io.github.reneknap.mediacenter.data.media

import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.TagReader
import io.github.reneknap.mediacenter.data.folder.FolderEntry
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.data.video.VideoItem
import io.github.reneknap.mediacenter.data.video.VideoMetadataReader
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
class MediaScanIndexImpl
    @Inject
    constructor(
        private val folderRepository: FolderRepository,
        private val scanner: MediaFileScanner,
        private val tagReader: TagReader,
        private val videoMetadataReader: VideoMetadataReader,
        @ApplicationScope private val scope: CoroutineScope,
    ) : MediaScanIndex {
        private val cache = MutableStateFlow<Map<String, MediaScanState>>(emptyMap())
        private val inFlight = mutableSetOf<String>()
        private val mutex = Mutex()

        init {
            scope.launch {
                folderRepository.folders.collect { entries -> syncScans(entries) }
            }
        }

        override val folders: Flow<List<FolderMedia>> =
            combine(
                folderRepository.folders,
                cache,
            ) { entries, cacheMap ->
                entries.map { entry ->
                    FolderMedia(entry, cacheMap[entry.uri] ?: MediaScanState.Scanning)
                }
            }.distinctUntilChanged()

        private suspend fun syncScans(entries: List<FolderEntry>) {
            mutex.withLock {
                val incomingUris = entries.map { it.uri }.toSet()
                cache.update { current -> current.filterKeys { it in incomingUris } }

                for (entry in entries) {
                    if (shouldSkip(entry)) continue
                    if (!entry.isReachable) {
                        cache.update { it + (entry.uri to MediaScanState.Unreachable) }
                        continue
                    }
                    inFlight += entry.uri
                    cache.update { it + (entry.uri to MediaScanState.Scanning) }
                    scope.launch { runScan(entry.uri) }
                }
            }
        }

        private fun shouldSkip(entry: FolderEntry): Boolean {
            if (entry.uri in inFlight) return true
            return cache.value[entry.uri] is MediaScanState.Ready
        }

        private suspend fun runScan(folderUri: String) {
            try {
                val result = scanner.scan(folderUri)
                val audio =
                    result.media
                        .filter { it.kind == MediaKind.AUDIO }
                        .map { toTrack(it, folderUri) }
                        .sortedBy { it.displayName.lowercase() }
                val video =
                    result.media
                        .filter { it.kind == MediaKind.VIDEO }
                        .map { toVideo(it, folderUri, result.subtitles) }
                        .sortedBy { it.displayName.lowercase() }
                cache.update { it + (folderUri to MediaScanState.Ready(audio, video)) }
            } catch (_: Exception) {
                // Defensive: any platform failure (SecurityException mid-scan, OOM on a huge tag,
                // unexpected DocumentsContract exception) falls back to Unreachable so the UI does
                // not stay stuck on Scanning forever.
                cache.update { it + (folderUri to MediaScanState.Unreachable) }
            } finally {
                mutex.withLock { inFlight -= folderUri }
            }
        }

        private suspend fun toTrack(
            file: RawMediaFile,
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

        private suspend fun toVideo(
            file: RawMediaFile,
            folderUri: String,
            subtitles: List<RawSubtitleFile>,
        ): VideoItem {
            // List every file with a video extension. Metadata is best-effort enrichment: some
            // containers (older AVI codecs) cannot be parsed by MediaMetadataRetriever, so a null
            // result leaves duration/resolution unknown rather than dropping a real video.
            val metadata = videoMetadataReader.read(file.uri)
            return VideoItem(
                uri = file.uri,
                folderUri = folderUri,
                displayName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                durationMs = metadata?.durationMs ?: 0L,
                width = metadata?.width ?: 0,
                height = metadata?.height ?: 0,
                externalSubtitles = matchSubtitles(file.displayName, file.parentKey, subtitles),
            )
        }
    }
