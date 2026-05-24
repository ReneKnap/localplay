package io.github.reneknap.mediacenter.ui.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.ui.components.StatusMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderTitle(uiState)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.video_player_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                VideoPlayerUiState.Loading -> CircularProgressIndicator()
                VideoPlayerUiState.NotAvailable -> NotAvailableState(onBack = onBack)
                is VideoPlayerUiState.EmptyFolder -> NoVideosState(onBack = onBack)
                is VideoPlayerUiState.Ready -> VideoSurface(player = viewModel.player, onPause = viewModel::pause)
            }
        }
    }
}

@Composable
private fun folderTitle(uiState: VideoPlayerUiState): String =
    when (uiState) {
        is VideoPlayerUiState.Ready -> uiState.folderName
        is VideoPlayerUiState.EmptyFolder -> uiState.folderName
        else -> ""
    }

@Composable
private fun NotAvailableState(onBack: () -> Unit) {
    StatusMessage(
        icon = Icons.Filled.Warning,
        title = stringResource(R.string.video_player_unavailable_title),
        description = stringResource(R.string.video_player_unavailable_message),
        iconTint = MaterialTheme.colorScheme.error,
        actionLabel = stringResource(R.string.status_back_to_folders),
        onAction = onBack,
    )
}

@Composable
private fun NoVideosState(onBack: () -> Unit) {
    StatusMessage(
        icon = Icons.Filled.VideocamOff,
        title = stringResource(R.string.video_player_no_videos_title),
        description = stringResource(R.string.video_player_no_videos_message),
        actionLabel = stringResource(R.string.status_back_to_folders),
        onAction = onBack,
    )
}

@Composable
private fun VideoSurface(
    player: Player?,
    onPause: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) onPause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    )
}
