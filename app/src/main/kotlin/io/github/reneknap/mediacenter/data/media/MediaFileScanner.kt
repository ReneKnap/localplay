package io.github.reneknap.mediacenter.data.media

interface MediaFileScanner {
    /**
     * Recursively walks [folderUri] once and returns every supported media file (classified by kind)
     * plus the subtitle sidecar files found alongside them.
     */
    suspend fun scan(folderUri: String): MediaScanResult
}
