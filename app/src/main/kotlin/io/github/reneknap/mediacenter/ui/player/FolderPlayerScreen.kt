package io.github.reneknap.mediacenter.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        onToggleShuffle = viewModel::toggleShuffle,
        onSeek = viewModel::seekTo,
        onLoadArtwork = viewModel::artworkFor,
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
    onToggleShuffle: () -> Unit,
    onSeek: (Long) -> Unit,
    onLoadArtwork: suspend (String) -> Bitmap?,
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
                    onToggleShuffle = onToggleShuffle,
                    onSeek = onSeek,
                    onLoadArtwork = onLoadArtwork,
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
    val listState = rememberLazyListState()
    LaunchedEffect(state.displayOrder) {
        val current = state.currentIndex ?: return@LaunchedEffect
        val position = state.displayOrder.indexOf(current).takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.animateScrollToItem(position)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(
            items = state.displayOrder,
            key = { _, trackIndex -> state.tracks[trackIndex].uri },
        ) { _, trackIndex ->
            val track = state.tracks[trackIndex]
            TrackRow(
                track = track,
                isSelected = state.selectedIndex == trackIndex,
                isHighlighted = highlightIndex == trackIndex,
                onClick = { onTrackSelected(trackIndex) },
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
    onToggleShuffle: () -> Unit,
    onSeek: (Long) -> Unit,
    onLoadArtwork: suspend (String) -> Bitmap?,
) {
    val displayIndex = state.selectedIndex ?: state.currentIndex
    val displayTrack = displayIndex?.let { state.tracks.getOrNull(it) }
    val isPendingSelection = displayIndex != null && displayIndex != state.currentIndex
    val positionMs = if (isPendingSelection) 0L else state.status.positionMs
    val durationMs = if (isPendingSelection) (displayTrack?.durationMs ?: 0L) else state.status.durationMs
    val isSeekable = displayIndex != null && !isPendingSelection && durationMs > 0L
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CurrentTrackInfo(displayTrack = displayTrack, onLoadArtwork = onLoadArtwork)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressRow(
                positionMs = positionMs,
                durationMs = durationMs,
                isSeekable = isSeekable,
                onSeek = onSeek,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TransportRow(
                state = state,
                onPlayOrToggle = onPlayOrToggle,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
            )
        }
    }
}

@Composable
private fun CurrentTrackInfo(
    displayTrack: AudioTrack?,
    onLoadArtwork: suspend (String) -> Bitmap?,
) {
    if (displayTrack == null) {
        Text(
            text = stringResource(R.string.player_no_track_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoverArt(trackUri = displayTrack.uri, onLoadArtwork = onLoadArtwork)
        Spacer(modifier = Modifier.size(12.dp))
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
}

@Composable
private fun CoverArt(
    trackUri: String,
    onLoadArtwork: suspend (String) -> Bitmap?,
) {
    val cover by produceState<Bitmap?>(initialValue = null, key1 = trackUri) {
        value = onLoadArtwork(trackUri)
    }
    val shape = RoundedCornerShape(8.dp)
    val bitmap = cover
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.player_cover_art),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(shape),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = stringResource(R.string.player_no_cover_art),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressRow(
    positionMs: Long,
    durationMs: Long,
    isSeekable: Boolean,
    onSeek: (Long) -> Unit,
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val progress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val sliderValue = dragValue ?: progress
    val displayedPositionMs = (sliderValue * durationMs).toLong()
    val seekBarDescription = stringResource(R.string.player_seek_bar)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeek((it * durationMs).toLong()) }
                dragValue = null
            },
            enabled = isSeekable,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = seekBarDescription },
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text =
                stringResource(
                    R.string.player_elapsed_of_total,
                    formatTime(displayedPositionMs),
                    formatDuration(durationMs),
                ),
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
    onToggleShuffle: () -> Unit,
) {
    val playEnabled = state.tracks.isNotEmpty()
    val showPause = state.status.isPlaying
    val shuffleDescription =
        if (state.shuffleEnabled) {
            stringResource(R.string.player_shuffle_on)
        } else {
            stringResource(R.string.player_shuffle_off)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = state.shuffleEnabled,
            onCheckedChange = { onToggleShuffle() },
            enabled = playEnabled,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.shuffleEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = shuffleDescription,
                    tint =
                        if (state.shuffleEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
        IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
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
        IconButton(onClick = onNext, enabled = state.hasNext) {
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
    return formatTime(durationMs)
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
