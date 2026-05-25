package io.github.reneknap.mediacenter.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.media.MediaEntry
import io.github.reneknap.mediacenter.data.media.MediaKind
import io.github.reneknap.mediacenter.ui.components.StatusMessage
import java.util.Locale

@Composable
fun FolderPlayerScreen(
    onBack: () -> Unit,
    viewModel: FolderPlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    FolderPlayerContent(
        uiState = uiState,
        player = player,
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
        onToggleFullscreen = viewModel::toggleFullscreen,
        onSelectSubtitle = viewModel::selectSubtitleTrack,
        onDisableSubtitles = viewModel::disableSubtitles,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPlayerContent(
    uiState: FolderPlayerUiState,
    player: Player?,
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
    onToggleFullscreen: () -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
) {
    if (uiState is FolderPlayerUiState.Ready && uiState.isFullscreen) {
        FullscreenVideo(
            player = player,
            state = uiState,
            onExitFullscreen = onToggleFullscreen,
            onPlayOrToggle = onPlayOrToggle,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onSelectSubtitle = onSelectSubtitle,
            onDisableSubtitles = onDisableSubtitles,
        )
        return
    }
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
                    onSelectSubtitle = onSelectSubtitle,
                    onDisableSubtitles = onDisableSubtitles,
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
                FolderPlayerUiState.NotAvailable -> NotAvailableState(onBack = onBack)
                is FolderPlayerUiState.EmptyFolder -> EmptyFolderState(onBack = onBack)
                is FolderPlayerUiState.Ready ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        // The list is anchored at the top; the inline video preview sits below it so the
                        // list does not jump as the current item alternates between audio and video — a
                        // video just shrinks the visible list area instead of pushing it down.
                        TrackList(
                            state = uiState,
                            onTrackSelected = onTrackSelected,
                            onMoveTrack = onMoveTrack,
                            onPlayNext = onPlayNext,
                            onDeactivateTrack = onDeactivateTrack,
                            onReactivateTrack = onReactivateTrack,
                            onReactivateAt = onReactivateAt,
                            onLoadThumbnail = onLoadThumbnail,
                            modifier = Modifier.weight(1f),
                        )
                        if (uiState.isCurrentVideo) {
                            InlineVideoPreview(player = player, onEnterFullscreen = onToggleFullscreen)
                        }
                    }
            }
        }
    }
}

@Composable
private fun folderTitle(uiState: FolderPlayerUiState): String =
    when (uiState) {
        is FolderPlayerUiState.Ready -> uiState.folderName
        is FolderPlayerUiState.EmptyFolder -> uiState.folderName
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
private fun NotAvailableState(onBack: () -> Unit) {
    StatusMessage(
        icon = Icons.Filled.Warning,
        title = stringResource(R.string.player_folder_unavailable),
        description = stringResource(R.string.player_folder_unavailable_message),
        iconTint = MaterialTheme.colorScheme.error,
        actionLabel = stringResource(R.string.status_back_to_folders),
        onAction = onBack,
    )
}

@Composable
private fun EmptyFolderState(onBack: () -> Unit) {
    StatusMessage(
        icon = Icons.Filled.MusicOff,
        title = stringResource(R.string.player_empty_folder_title),
        description = stringResource(R.string.player_empty_folder_message),
        actionLabel = stringResource(R.string.status_back_to_folders),
        onAction = onBack,
    )
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
    modifier: Modifier = Modifier,
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
                onReactivateAt = { entryIndex, position -> latestOnReactivateAt(entryIndex, position) },
            )
        }
    dragDropState.activeCount = activeCount

    // Scroll to the current entry only when it actually changes (auto-advance, play, prev/next) — not on
    // every reorder/deactivate/reactivate, which would yank the list away from what the user is editing.
    LaunchedEffect(state.currentIndex) {
        if (dragDropState.draggingItemIndex != null) return@LaunchedEffect
        val current = state.currentIndex ?: return@LaunchedEffect
        val position = state.displayOrder.indexOf(current).takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.animateScrollToItem(position)
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        itemsIndexed(
            items = state.displayOrder,
            key = { _, entryIndex -> state.entries[entryIndex].uri },
        ) { displayPosition, entryIndex ->
            val entry = state.entries[entryIndex]
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
                    entry = entry,
                    isSelected = state.selectedIndex == entryIndex,
                    isHighlighted = highlightIndex == entryIndex,
                    onClick = { onTrackSelected(entryIndex) },
                    onPlayNext = { onPlayNext(currentPosition) },
                    onRemove = { if (canDeactivate) onDeactivateTrack(currentPosition) },
                    onLoadThumbnail = onLoadThumbnail,
                    dragHandleModifier = dragHandleModifier,
                )
            }
        }
        itemsIndexed(
            items = state.deactivatedOrder,
            key = { _, entryIndex -> state.entries[entryIndex].uri },
        ) { deactivatedPosition, entryIndex ->
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
                        onDragStart = { dragDropState.onDeactivatedDragStart(currentLazyIndex, entryIndex) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount.y)
                        },
                        onDragEnd = { dragDropState.onDragEnd() },
                        onDragCancel = { dragDropState.onDragEnd() },
                    )
                }
            DeactivatedRow(
                entry = state.entries[entryIndex],
                showHeader = deactivatedPosition == 0,
                onReactivate = { onReactivateTrack(entryIndex) },
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
    entry: MediaEntry,
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
                EntryThumbnail(
                    entry = entry,
                    isHighlighted = isHighlighted,
                    onLoadThumbnail = onLoadThumbnail,
                )
            },
            headlineContent = {
                Text(
                    text = entryTitle(entry),
                    color = titleColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = { EntrySubtitle(entry) },
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
    entry: MediaEntry,
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
                EntryThumbnail(
                    entry = entry,
                    isHighlighted = false,
                    onLoadThumbnail = onLoadThumbnail,
                )
            },
            headlineContent = {
                Text(
                    text = entryTitle(entry),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = { EntrySubtitle(entry) },
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

private fun entryTitle(entry: MediaEntry): String =
    when (entry) {
        is MediaEntry.Audio -> entry.track.title
        is MediaEntry.Video -> entry.video.displayName
    }

@Composable
private fun EntrySubtitle(entry: MediaEntry) {
    val text =
        when (entry) {
            is MediaEntry.Audio -> {
                val artist = entry.track.artist
                if (!artist.isNullOrBlank()) {
                    "$artist  •  ${formatDuration(entry.durationMs)}"
                } else {
                    formatDuration(entry.durationMs)
                }
            }
            is MediaEntry.Video -> {
                val video = entry.video
                if (video.width > 0 && video.height > 0) {
                    "${video.width}×${video.height}  •  ${formatDuration(entry.durationMs)}"
                } else {
                    formatDuration(entry.durationMs)
                }
            }
        }
    Text(text = text, style = MaterialTheme.typography.bodySmall)
}

private fun placeholderIconFor(kind: MediaKind) =
    when (kind) {
        MediaKind.AUDIO -> Icons.Filled.MusicNote
        MediaKind.VIDEO -> Icons.Filled.Movie
    }

@Composable
private fun EntryThumbnail(
    entry: MediaEntry,
    isHighlighted: Boolean,
    onLoadThumbnail: suspend (String) -> Bitmap?,
) {
    val cover by produceState<Bitmap?>(initialValue = null, key1 = entry.uri) {
        value = onLoadThumbnail(entry.uri)
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
                imageVector = placeholderIconFor(entry.kind),
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
 * Hand-rolled drag state for the entry list (ADR-008: no reorder dependency). Two modes:
 *
 * - **Active drag** (a row in the active section, index < [activeCount]): continuous reorder — crossing
 *   an active neighbour's midpoint triggers [onMove] and the dragged index follows the item. Targets are
 *   clamped to the active section.
 * - **Deactivated drag** (a greyed row): drop-commit — the row only translates visually while dragging;
 *   on release, if it landed over the active section, [onReactivateAt] re-adds the entry at that position.
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

    private var draggingEntryIndex: Int? = null
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

    fun onActiveDragStart(index: Int) = start(index, fromDeactivated = false, entryIndex = null)

    fun onDeactivatedDragStart(
        index: Int,
        entryIndex: Int,
    ) = start(index, fromDeactivated = true, entryIndex = entryIndex)

    private fun start(
        index: Int,
        fromDeactivated: Boolean,
        entryIndex: Int?,
    ) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        draggingItemIndex = index
        draggingItemInitialOffset = info.offset
        draggingDelta = 0f
        this.fromDeactivated = fromDeactivated
        draggingEntryIndex = entryIndex
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
        draggingEntryIndex = null
        fromDeactivated = false
        draggingDelta = 0f
        draggingItemInitialOffset = 0
    }

    private fun dropDeactivatedIntoActive() {
        val dragging = currentItemInfo ?: return
        val entryIndex = draggingEntryIndex ?: return
        val center = (draggingItemInitialOffset + draggingDelta + dragging.size / 2f).toInt()
        val activeItems = listState.layoutInfo.visibleItemsInfo.filter { it.index < activeCount }
        val targetPosition =
            when {
                activeItems.isEmpty() -> null
                center < activeItems.first().offset -> 0
                else -> activeItems.firstOrNull { center in it.offset..(it.offset + it.size) }?.index
            }
        if (targetPosition != null) onReactivateAt(entryIndex, targetPosition)
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
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
) {
    val displayIndex = state.selectedIndex ?: state.currentIndex
    val displayEntry = displayIndex?.let { state.entries.getOrNull(it) }
    val isPendingSelection = displayIndex != null && displayIndex != state.currentIndex
    val positionMs = if (isPendingSelection) 0L else state.status.positionMs
    val durationMs = if (isPendingSelection) (displayEntry?.durationMs ?: 0L) else state.status.durationMs
    val isSeekable = displayIndex != null && !isPendingSelection && durationMs > 0L
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CurrentTrackInfo(displayEntry = displayEntry, onLoadArtwork = onLoadArtwork)
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
                onSelectSubtitle = onSelectSubtitle,
                onDisableSubtitles = onDisableSubtitles,
            )
        }
    }
}

@Composable
private fun CurrentTrackInfo(
    displayEntry: MediaEntry?,
    onLoadArtwork: suspend (String) -> Bitmap?,
) {
    if (displayEntry == null) {
        Text(
            text = stringResource(R.string.player_no_track_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoverArt(entry = displayEntry, onLoadArtwork = onLoadArtwork)
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = entryTitle(displayEntry),
                style = MaterialTheme.typography.titleMedium,
            )
            val subtitle = currentEntrySubtitle(displayEntry)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun currentEntrySubtitle(entry: MediaEntry): String? =
    when (entry) {
        is MediaEntry.Audio -> entry.track.artist?.takeIf { it.isNotBlank() }
        is MediaEntry.Video -> entry.video.takeIf { it.width > 0 && it.height > 0 }?.let { "${it.width}×${it.height}" }
    }

@Composable
private fun CoverArt(
    entry: MediaEntry,
    onLoadArtwork: suspend (String) -> Bitmap?,
) {
    val cover by produceState<Bitmap?>(initialValue = null, key1 = entry.uri) {
        value = onLoadArtwork(entry.uri)
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
                imageVector = placeholderIconFor(entry.kind),
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
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
) {
    val playEnabled = state.entries.isNotEmpty()
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
        SubtitleControl(
            state = state,
            onSelectSubtitle = onSelectSubtitle,
            onDisableSubtitles = onDisableSubtitles,
        )
    }
}

/**
 * Closed-caption control: only shown for a video with subtitle tracks. Opens a picker of "Off" plus
 * each track; the active one is checked. Reused inline and in the fullscreen overlay.
 */
@Composable
private fun SubtitleControl(
    state: FolderPlayerUiState.Ready,
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
    tint: Color = LocalContentColor.current,
) {
    if (!state.isCurrentVideo || state.subtitleTracks.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val activeId = state.activeSubtitleTrackId
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = if (activeId != null) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                contentDescription = stringResource(R.string.player_subtitles),
                tint = tint,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_subtitles_off)) },
                onClick = {
                    onDisableSubtitles()
                    expanded = false
                },
                trailingIcon = { if (activeId == null) SelectedCheck() },
            )
            state.subtitleTracks.forEach { track ->
                DropdownMenuItem(
                    text = { Text(track.label) },
                    onClick = {
                        onSelectSubtitle(track.id)
                        expanded = false
                    },
                    trailingIcon = { if (track.id == activeId) SelectedCheck() },
                )
            }
        }
    }
}

@Composable
private fun SelectedCheck() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = stringResource(R.string.player_subtitles_selected),
    )
}

@Composable
private fun VideoSurface(
    player: Player?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier.background(Color.Black),
    )
}

@Composable
private fun InlineVideoPreview(
    player: Player?,
    onEnterFullscreen: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
    ) {
        VideoSurface(player = player, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = onEnterFullscreen,
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Icon(
                imageVector = Icons.Filled.Fullscreen,
                contentDescription = stringResource(R.string.player_enter_fullscreen),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FullscreenVideo(
    player: Player?,
    state: FolderPlayerUiState.Ready,
    onExitFullscreen: () -> Unit,
    onPlayOrToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
) {
    ImmersiveLandscapeEffect()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        VideoSurface(player = player, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = onExitFullscreen,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.Filled.FullscreenExit,
                contentDescription = stringResource(R.string.player_exit_fullscreen),
                tint = Color.White,
            )
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ProgressRow(
                positionMs = state.status.positionMs,
                durationMs = state.status.durationMs,
                isSeekable = state.status.durationMs > 0L,
                onSeek = onSeek,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.player_previous),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onPlayOrToggle) {
                    Icon(
                        imageVector = if (state.status.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription =
                            if (state.status.isPlaying) {
                                stringResource(R.string.player_pause)
                            } else {
                                stringResource(R.string.player_play)
                            },
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.player_next),
                        tint = Color.White,
                    )
                }
                SubtitleControl(
                    state = state,
                    onSelectSubtitle = onSelectSubtitle,
                    onDisableSubtitles = onDisableSubtitles,
                    tint = Color.White,
                )
            }
        }
    }
}

/** Hides the system bars and locks landscape while composed; restores both on dispose (ADR-010). */
@Composable
private fun ImmersiveLandscapeEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
