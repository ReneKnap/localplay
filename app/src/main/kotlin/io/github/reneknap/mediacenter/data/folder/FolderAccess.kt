package io.github.reneknap.mediacenter.data.folder

interface FolderAccess {
    fun takePersistable(uri: String)

    fun releasePersistable(uri: String)

    fun canRead(uri: String): Boolean

    fun displayName(uri: String): String?
}
