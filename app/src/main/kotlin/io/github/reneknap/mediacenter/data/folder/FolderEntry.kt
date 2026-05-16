package io.github.reneknap.mediacenter.data.folder

import kotlinx.serialization.Serializable

@Serializable
data class FolderEntry(
    val uri: String,
    val displayName: String,
    val isReachable: Boolean = true,
)
