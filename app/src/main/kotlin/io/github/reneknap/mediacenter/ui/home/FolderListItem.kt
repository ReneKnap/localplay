package io.github.reneknap.mediacenter.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import io.github.reneknap.mediacenter.data.audio.FolderScanState
import io.github.reneknap.mediacenter.data.video.VideoItem
import io.github.reneknap.mediacenter.data.video.VideoScanState
import java.util.Locale

@Composable
fun FolderListItem(
    folderMedia: FolderMediaUi,
    onRemove: () -> Unit,
    onFolderClick: () -> Unit,
    onPreviewTrackClick: (trackUri: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(folderMedia.folder.uri) { mutableStateOf(false) }
    val folder = folderMedia.folder
    val tracks = (folderMedia.audio as? FolderScanState.Ready)?.tracks.orEmpty()
    val videos = (folderMedia.video as? VideoScanState.Ready)?.videos.orEmpty()
    val canExpand = tracks.isNotEmpty() || videos.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClick = onFolderClick),
            headlineContent = { Text(folder.displayName) },
            supportingContent = { ScanSummary(folderMedia.audio, folderMedia.video) },
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
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                            )
                        }
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
        if (expanded && canExpand) {
            tracks.forEach { track ->
                PreviewTrackRow(
                    track = track,
                    onClick = { onPreviewTrackClick(track.uri) },
                )
            }
            if (videos.isNotEmpty()) {
                SectionCaption(stringResource(R.string.home_folder_videos_section))
                videos.forEach { video ->
                    VideoRow(video = video)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ScanSummary(
    audio: FolderScanState,
    video: VideoScanState,
) {
    when {
        audio is FolderScanState.Unreachable || video is VideoScanState.Unreachable ->
            Text(
                text = stringResource(R.string.home_folder_not_accessible),
                color = MaterialTheme.colorScheme.error,
            )
        audio is FolderScanState.Ready && video is VideoScanState.Ready ->
            Text(
                text =
                    stringResource(
                        R.string.home_folder_media_summary,
                        audioCountLabel(audio.tracks.size),
                        videoCountLabel(video.videos.size),
                    ),
            )
        else -> Text(stringResource(R.string.home_folder_scanning))
    }
}

@Composable
private fun audioCountLabel(count: Int): String =
    when (count) {
        0 -> stringResource(R.string.home_folder_no_audio)
        1 -> stringResource(R.string.home_folder_track_count_one)
        else -> stringResource(R.string.home_folder_track_count, count)
    }

@Composable
private fun videoCountLabel(count: Int): String =
    when (count) {
        0 -> stringResource(R.string.home_folder_no_videos)
        1 -> stringResource(R.string.home_folder_video_count_one)
        else -> stringResource(R.string.home_folder_video_count, count)
    }

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PreviewTrackRow(
    track: AudioTrack,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(start = 56.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VideoRow(video: VideoItem) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 56.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = video.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.size(8.dp))
        if (video.width > 0 && video.height > 0) {
            Text(
                text = stringResource(R.string.home_video_resolution, video.width, video.height),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = formatDuration(video.durationMs),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return stringResource(R.string.home_track_unknown_duration)
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
