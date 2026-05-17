package io.github.reneknap.mediacenter.data.audio

interface AudioFileScanner {
    suspend fun scan(folderUri: String): List<RawAudioFile>
}
