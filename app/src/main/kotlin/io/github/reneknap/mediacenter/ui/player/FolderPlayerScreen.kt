package io.github.reneknap.mediacenter.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
        onLoadThumbnail = viewModel::thumbnailFor,
        onMoveTrack = viewModel::moveTrack,
        onPlayNext = viewModel::playTrackNext,
        onDeactivateTrack = viewModel::deactivateTrack,
        onReactivateTrack = viewModel::reactivateTrack,
        onReactivateAt = viewModel::reactivateTrackAt,
        onResetQueue = viewModel::resetQueue,
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
    onLoadThumbnail: suspend (String) -> Bitmap?,
    onMoveTrack: (Int, Int) -> Unit,
    onPlayNext: (Int) -> Unit,
    onDeactivateTrack: (Int) -> Unit,
    onReactivateTrack: (Int) -> Unit,
    onReactivateAt: (Int, Int) -> Unit,
    onResetQueue: () -> Unit,
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
                actions = {
                    if (uiState is FolderPlayerUiState.Ready) {
                        IconButton(onClick = onResetQueue) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = stringResource(R.string.player_reset_queue),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
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
                        onMoveTrack = onMoveTrack,
                        onPlayNext = onPlayNext,
                        onDeactivateTrack = onDeactivateTrack,
                        onReactivateTrack = onReactivateTrack,
                        onReactivateAt = onReactivateAt,
                        onLoadThumbnail = onLoadThumbnail,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackList(
    state: FolderPlayerUiState.Ready,
    onTrackSelected: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onPlayNext: (Int) -> Unit,
    onDeactivateTrack: (Int) -> Unit,
    onReactivateTrack: (Int) -> Unit,
    onReactivateAt: (Int, Int) -> Unit,
    onLoadThumbnail: suspend (String) -> Bitmap?,
) {
    val highlightIndex = state.selectedIndex ?: state.currentIndex
    val activeCount = state.displayOrder.size
    val canDeactivate = activeCount > 1
    val listState = rememberLazyListState()
    val latestOnMove by rememberUpdatedState(onMoveTrack)
    val latestOnReactivateAt by rememberUpdatedState(onReactivateAt)
    val dragDropState =
        remember(listState) {
            DragDropState(
                listState = listState,
                onMove = { from, to -> latestOnMove(from, to) },
                onReactivateAt = { trackIndex, position -> latestOnReactivateAt(trackIndex, position) },
            )
        }
    dragDropState.activeCount = activeCount

    // Scroll to the current track only when it actually changes (auto-advance, play, prev/next) — not on
    // every reorder/deactivate/reactivate, which would yank the list away from what the user is editing.
    LaunchedEffect(state.currentIndex) {
        if (dragDropState.draggingItemIndex != null) return@LaunchedEffect
        val current = state.currentIndex ?: return@LaunchedEffect
        val position = state.displayOrder.indexOf(current).takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.animateScrollToItem(position)
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(
            items = state.displayOrder,
            key = { _, trackIndex -> state.tracks[trackIndex].uri },
        ) { displayPosition, trackIndex ->
            val track = state.tracks[trackIndex]
            val isDragging = displayPosition == dragDropState.draggingItemIndex
            val currentPosition by rememberUpdatedState(displayPosition)
            val dismissState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled && canDeactivate) {
                            onDeactivateTrack(currentPosition)
                            true
                        } else {
                            false
                        }
                    },
                )
            val itemModifier =
                if (isDragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer { translationY = dragDropState.draggingItemOffset }
                } else {
                    Modifier.animateItem()
                }
            val dragHandleModifier =
                Modifier.pointerInput(dragDropState) {
                    detectDragGestures(
                        onDragStart = { dragDropState.onActiveDragStart(currentPosition) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount.y)
                        },
                        onDragEnd = { dragDropState.onDragEnd() },
                        onDragCancel = { dragDropState.onDragEnd() },
                    )
                }
            SwipeToDismissBox(
                state = dismissState,
                modifier = itemModifier,
                backgroundContent = { SwipeRemoveBackground(dismissState.dismissDirection) },
            ) {
                TrackRow(
                    track = track,
                    isSelected = state.selectedIndex == trackIndex,
                    isHighlighted = highlightIndex == trackIndex,
                    onClick = { onTrackSelected(trackIndex) },
                    onPlayNext = { onPlayNext(currentPosition) },
                    onRemove = { if (canDeactivate) onDeactivateTrack(currentPosition) },
                    onLoadThumbnail = onLoadThumbnail,
                    dragHandleModifier = dragHandleModifier,
                )
            }
        }
        itemsIndexed(
            items = state.deactivatedOrder,
            key = { _, trackIndex -> state.tracks[trackIndex].uri },
        ) { deactivatedPosition, trackIndex ->
            val lazyIndex = activeCount + deactivatedPosition
            val isDragging = lazyIndex == dragDropState.draggingItemIndex
            val currentLazyIndex by rememberUpdatedState(lazyIndex)
            val itemModifier =
                if (isDragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer { translationY = dragDropState.draggingItemOffset }
                } else {
                    Modifier.animateItem()
                }
            val dragHandleModifier =
                Modifier.pointerInput(dragDropState) {
                    detectDragGestures(
                        onDragStart = { dragDropState.onDeactivatedDragStart(currentLazyIndex, trackIndex) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount.y)
                        },
                        onDragEnd = { dragDropState.onDragEnd() },
                        onDragCancel = { dragDropState.onDragEnd() },
                    )
                }
            DeactivatedRow(
                track = state.tracks[trackIndex],
                showHeader = deactivatedPosition == 0,
                onReactivate = { onReactivateTrack(trackIndex) },
                onLoadThumbnail = onLoadThumbnail,
                dragHandleModifier = dragHandleModifier,
                modifier = itemModifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TrackRow(
    track: AudioTrack,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onLoadThumbnail: suspend (String) -> Bitmap?,
    dragHandleModifier: Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
    Box {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(rowBackground)
                    .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true }),
            colors = ListItemDefaults.colors(containerColor = rowBackground),
            leadingContent = {
                TrackThumbnail(
                    trackUri = track.uri,
                    isHighlighted = isHighlighted,
                    onLoadThumbnail = onLoadThumbnail,
                )
            },
            headlineContent = {
                Text(
                    text = track.title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = { TrackSubtitle(track) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.player_reorder_handle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier,
                )
            },
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_play_next)) },
                onClick = {
                    menuExpanded = false
                    onPlayNext()
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_remove_from_queue)) },
                onClick = {
                    menuExpanded = false
                    onRemove()
                },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeactivatedRow(
    track: AudioTrack,
    showHeader: Boolean,
    onReactivate: () -> Unit,
    onLoadThumbnail: suspend (String) -> Bitmap?,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Text(
                text = stringResource(R.string.player_deactivated_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReactivate)
                    .alpha(0.5f),
            leadingContent = {
                TrackThumbnail(
                    trackUri = track.uri,
                    isHighlighted = false,
                    onLoadThumbnail = onLoadThumbnail,
                )
            },
            headlineContent = {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = { TrackSubtitle(track) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.player_reactivate),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier,
                )
            },
        )
    }
}

@Composable
private fun TrackSubtitle(track: AudioTrack) {
    val artist = track.artist
    Text(
        text =
            if (!artist.isNullOrBlank()) {
                "$artist  •  ${formatDuration(track.durationMs)}"
            } else {
                formatDuration(track.durationMs)
            },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun TrackThumbnail(
    trackUri: String,
    isHighlighted: Boolean,
    onLoadThumbnail: suspend (String) -> Bitmap?,
) {
    val cover by produceState<Bitmap?>(initialValue = null, key1 = trackUri) {
        value = onLoadThumbnail(trackUri)
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = cover
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.player_cover_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = stringResource(R.string.player_no_cover_art),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isHighlighted) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = stringResource(R.string.player_now_playing_content_description),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRemoveBackground(direction: SwipeToDismissBoxValue) {
    val alignment =
        if (direction == SwipeToDismissBoxValue.StartToEnd) {
            Alignment.CenterStart
        } else {
            Alignment.CenterEnd
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.player_remove_from_queue),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/**
 * Hand-rolled drag state for the track list (ADR-008: no reorder dependency). Two modes:
 *
 * - **Active drag** (a row in the active section, index < [activeCount]): continuous reorder — crossing
 *   an active neighbour's midpoint triggers [onMove] and the dragged index follows the item. Targets are
 *   clamped to the active section.
 * - **Deactivated drag** (a greyed row): drop-commit — the row only translates visually while dragging;
 *   on release, if it landed over the active section, [onReactivateAt] re-adds the track at that position.
 *
 * The dragged item is always translated to follow the finger via [draggingItemOffset].
 */
private class DragDropState(
    private val listState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val onReactivateAt: (Int, Int) -> Unit,
) {
    var activeCount: Int = 0

    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var draggingTrackIndex: Int? = null
    private var fromDeactivated = false
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    private var draggingDelta by mutableFloatStateOf(0f)

    val draggingItemOffset: Float
        get() =
            currentItemInfo?.let { info ->
                draggingItemInitialOffset + draggingDelta - info.offset
            } ?: 0f

    private val currentItemInfo
        get() =
            draggingItemIndex?.let { index ->
                listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }

    fun onActiveDragStart(index: Int) = start(index, fromDeactivated = false, trackIndex = null)

    fun onDeactivatedDragStart(
        index: Int,
        trackIndex: Int,
    ) = start(index, fromDeactivated = true, trackIndex = trackIndex)

    private fun start(
        index: Int,
        fromDeactivated: Boolean,
        trackIndex: Int?,
    ) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        draggingItemIndex = index
        draggingItemInitialOffset = info.offset
        draggingDelta = 0f
        this.fromDeactivated = fromDeactivated
        draggingTrackIndex = trackIndex
    }

    fun onDrag(deltaY: Float) {
        draggingDelta += deltaY
        if (fromDeactivated) return // drop-commit: translate visually, decide the target on release
        val dragging = currentItemInfo ?: return
        val center = draggingItemInitialOffset + draggingDelta + dragging.size / 2f
        val target =
            listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.index != dragging.index &&
                    item.index < activeCount &&
                    center.toInt() in item.offset..(item.offset + item.size)
            } ?: return
        onMove(dragging.index, target.index)
        draggingItemIndex = target.index
    }

    fun onDragEnd() {
        if (fromDeactivated) {
            dropDeactivatedIntoActive()
        }
        draggingItemIndex = null
        draggingTrackIndex = null
        fromDeactivated = false
        draggingDelta = 0f
        draggingItemInitialOffset = 0
    }

    private fun dropDeactivatedIntoActive() {
        val dragging = currentItemInfo ?: return
        val trackIndex = draggingTrackIndex ?: return
        val center = (draggingItemInitialOffset + draggingDelta + dragging.size / 2f).toInt()
        val activeItems = listState.layoutInfo.visibleItemsInfo.filter { it.index < activeCount }
        val targetPosition =
            when {
                activeItems.isEmpty() -> null
                center < activeItems.first().offset -> 0
                else -> activeItems.firstOrNull { center in it.offset..(it.offset + it.size) }?.index
            }
        if (targetPosition != null) onReactivateAt(trackIndex, targetPosition)
    }
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
