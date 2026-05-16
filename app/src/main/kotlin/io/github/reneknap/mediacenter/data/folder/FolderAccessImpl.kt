package io.github.reneknap.mediacenter.data.folder

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FolderAccessImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : FolderAccess {

    override fun takePersistable(uri: String) {
        context.contentResolver.takePersistableUriPermission(
            uri.toUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun releasePersistable(uri: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Permission was never held for this URI; nothing to release.
        }
    }

    override fun canRead(uri: String): Boolean =
        DocumentFile.fromTreeUri(context, uri.toUri())?.canRead() == true

    override fun displayName(uri: String): String? =
        DocumentFile.fromTreeUri(context, uri.toUri())?.name
}
