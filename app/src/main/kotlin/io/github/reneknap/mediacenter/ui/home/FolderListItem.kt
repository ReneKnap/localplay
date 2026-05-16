package io.github.reneknap.mediacenter.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.folder.FolderEntry

@Composable
fun FolderListItem(
    folder: FolderEntry,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(folder.displayName) },
        supportingContent = if (folder.isReachable) {
            null
        } else {
            {
                Text(
                    text = stringResource(R.string.home_folder_not_accessible),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (folder.isReachable) Icons.Filled.Folder else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (folder.isReachable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.home_remove_folder),
                )
            }
        },
    )
}
