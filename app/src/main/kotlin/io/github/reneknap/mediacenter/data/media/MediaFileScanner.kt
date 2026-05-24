package io.github.reneknap.mediacenter.data.media

interface MediaFileScanner {
    /** Recursively walks [folderUri] once and returns every supported media file, classified by kind. */
    suspend fun scan(folderUri: String): List<RawMediaFile>
}
