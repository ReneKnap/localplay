package io.github.reneknap.mediacenter.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.audio.AudioTrack
import java.util.Locale

@Composable
fun FolderPlayerScreen(
    onBack: () -> Unit,
    viewModel: FolderPlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FolderPlayerContent(
        uiState = uiState,
        onBack = onBack,
        onTrackSelected = viewModel::selectTrack,
        onPlayOrToggle = {
            val state = uiState
            val wantsToSwitchTrack =
                state is FolderPlayerUiState.Ready &&
                    state.selectedIndex != null &&
                    state.selectedIndex != state.currentIndex
            when {
                wantsToSwitchTrack -> viewModel.play()
                state is FolderPlayerUiState.Ready && state.currentIndex != null -> viewModel.togglePlayPause()
                else -> viewModel.play()
            }
        },
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPlayerContent(
    uiState: FolderPlayerUiState,
    onBack: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onPlayOrToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderTitle(uiState)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.player_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is FolderPlayerUiState.Ready) {
                PlayerControls(
                    state = uiState,
                    onPlayOrToggle = onPlayOrToggle,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (uiState) {
                FolderPlayerUiState.Loading -> LoadingState()
                FolderPlayerUiState.NotAvailable -> NotAvailableState()
                is FolderPlayerUiState.Ready ->
                    TrackList(
                        state = uiState,
                        onTrackSelected = onTrackSelected,
                    )
            }
        }
    }
}

@Composable
private fun folderTitle(uiState: FolderPlayerUiState): String =
    when (uiState) {
        is FolderPlayerUiState.Ready -> uiState.folderName
        else -> ""
    }

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotAvailableState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.player_folder_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrackList(
    state: FolderPlayerUiState.Ready,
    onTrackSelected: (Int) -> Unit,
) {
    val highlightIndex = state.selectedIndex ?: state.currentIndex
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = state.tracks, key = { _, track -> track.uri }) { index, track ->
            TrackRow(
                track = track,
                isSelected = state.selectedIndex == index,
                isHighlighted = highlightIndex == index,
                onClick = { onTrackSelected(index) },
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: AudioTrack,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground =
        if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val titleColor =
        if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = rowBackground),
        headlineContent = {
            Text(
                text = track.title,
                color = titleColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            val artist = track.artist
            if (!artist.isNullOrBlank()) {
                Text(
                    text = "$artist  •  ${formatDuration(track.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = formatDuration(track.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        leadingContent = {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = stringResource(R.string.player_now_playing_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        },
    )
}

@Composable
private fun PlayerControls(
    state: FolderPlayerUiState.Ready,
    onPlayOrToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val displayIndex = state.selectedIndex ?: state.currentIndex
    val displayTrack = displayIndex?.let { state.tracks.getOrNull(it) }
    val isPendingSelection = displayIndex != null && displayIndex != state.currentIndex
    val positionMs = if (isPendingSelection) 0L else state.status.positionMs
    val durationMs = if (isPendingSelection) (displayTrack?.durationMs ?: 0L) else state.status.durationMs
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CurrentTrackInfo(displayTrack = displayTrack)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressRow(positionMs = positionMs, durationMs = durationMs)
            Spacer(modifier = Modifier.height(8.dp))
            TransportRow(
                state = state,
                onPlayOrToggle = onPlayOrToggle,
                onNext = onNext,
                onPrevious = onPrevious,
            )
        }
    }
}

@Composable
private fun CurrentTrackInfo(displayTrack: AudioTrack?) {
    if (displayTrack == null) {
        Text(
            text = stringResource(R.string.player_no_track_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    Column {
        Text(
            text = displayTrack.title,
            style = MaterialTheme.typography.titleMedium,
        )
        val artist = displayTrack.artist
        if (!artist.isNullOrBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProgressRow(
    positionMs: Long,
    durationMs: Long,
) {
    val progress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = formatRemaining(positionMs, durationMs),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TransportRow(
    state: FolderPlayerUiState.Ready,
    onPlayOrToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val current = state.currentIndex
    val hasPrevious = current != null && current > 0
    val hasNext = current != null && current < state.tracks.lastIndex
    val playEnabled = state.tracks.isNotEmpty()
    val showPause = state.status.isPlaying
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
            )
        }
        IconButton(onClick = onPlayOrToggle, enabled = playEnabled) {
            Icon(
                imageVector = if (showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription =
                    if (showPause) {
                        stringResource(R.string.player_pause)
                    } else {
                        stringResource(R.string.player_play)
                    },
            )
        }
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.player_next),
            )
        }
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

@Composable
private fun formatRemaining(
    positionMs: Long,
    durationMs: Long,
): String {
    if (durationMs <= 0L) return stringResource(R.string.home_track_unknown_duration)
    val remaining = (durationMs - positionMs).coerceAtLeast(0L)
    val totalSeconds = remaining / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return stringResource(R.string.player_remaining_time, minutes, seconds)
}
