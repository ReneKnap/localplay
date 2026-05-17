package io.github.reneknap.mediacenter.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import java.util.Locale

@Composable
fun FolderListItem(
    folderTracks: FolderTracks,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(folderTracks.folder.uri) { mutableStateOf(false) }
    val folder = folderTracks.folder
    val tracks = (folderTracks.scan as? FolderScanState.Ready)?.tracks.orEmpty()
    val canExpand = folderTracks.scan is FolderScanState.Ready && tracks.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier,
            headlineContent = { Text(folder.displayName) },
            supportingContent = { ScanSummary(folderTracks.scan) },
            leadingContent = {
                Icon(
                    imageVector = if (folder.isReachable) Icons.Filled.Folder else Icons.Filled.Warning,
                    contentDescription = null,
                    tint =
                        if (folder.isReachable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (canExpand) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.home_remove_folder),
                        )
                    }
                }
            },
        )
        if (expanded && tracks.isNotEmpty()) {
            tracks.forEach { track -> TrackRow(track) }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ScanSummary(state: FolderScanState) {
    when (state) {
        FolderScanState.Scanning -> Text(stringResource(R.string.home_folder_scanning))
        FolderScanState.Unreachable ->
            Text(
                text = stringResource(R.string.home_folder_not_accessible),
                color = MaterialTheme.colorScheme.error,
            )
        is FolderScanState.Ready -> Text(text = trackCountLabel(state.tracks.size))
    }
}

@Composable
private fun trackCountLabel(count: Int): String =
    when (count) {
        0 -> stringResource(R.string.home_folder_no_audio)
        1 -> stringResource(R.string.home_folder_track_count_one)
        else -> stringResource(R.string.home_folder_track_count, count)
    }

@Composable
private fun TrackRow(track: AudioTrack) {
    ListItem(
        modifier = Modifier.padding(start = 32.dp),
        headlineContent = {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}

@Composable
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return stringResource(R.string.home_track_unknown_duration)
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
