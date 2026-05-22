package io.github.reneknap.mediacenter.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.audio.FolderTracks
import io.github.reneknap.mediacenter.data.theme.ThemeMode
import io.github.reneknap.mediacenter.ui.components.StatusMessage

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onFolderClick: (String) -> Unit,
    onPreviewTrackClick: (folderUri: String, trackUri: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let { viewModel.addFolder(it.toString()) }
        }

    HomeContent(
        uiState = uiState,
        themeMode = themeMode,
        onToggleTheme = onToggleTheme,
        onPickFolder = { pickerLauncher.launch(null) },
        onRemoveFolder = viewModel::removeFolder,
        onFolderClick = onFolderClick,
        onPreviewTrackClick = onPreviewTrackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onPickFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onPreviewTrackClick: (folderUri: String, trackUri: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    ThemeToggleButton(themeMode = themeMode, onToggleTheme = onToggleTheme)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        floatingActionButton = {
            if (uiState is HomeUiState.Folders) {
                FloatingActionButton(onClick = onPickFolder) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.home_add_folder),
                    )
                }
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
                HomeUiState.Loading -> LoadingState()
                HomeUiState.Empty -> EmptyState(onPickFolder = onPickFolder)
                is HomeUiState.Folders ->
                    FolderList(
                        folders = uiState.items,
                        onRemove = onRemoveFolder,
                        onFolderClick = onFolderClick,
                        onPreviewTrackClick = onPreviewTrackClick,
                    )
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
) {
    val (icon, descriptionRes) =
        when (themeMode) {
            ThemeMode.DARK -> Icons.Filled.DarkMode to R.string.theme_toggle_dark
            ThemeMode.LIGHT -> Icons.Filled.LightMode to R.string.theme_toggle_light
            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto to R.string.theme_toggle_system
        }
    IconButton(onClick = onToggleTheme) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(descriptionRes),
        )
    }
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
private fun EmptyState(onPickFolder: () -> Unit) {
    StatusMessage(
        icon = Icons.Filled.LibraryMusic,
        title = stringResource(R.string.home_empty_title),
        description = stringResource(R.string.home_empty_explanation),
        iconTint = MaterialTheme.colorScheme.primary,
        actionLabel = stringResource(R.string.home_pick_folder_button),
        onAction = onPickFolder,
    )
}

@Composable
private fun FolderList(
    folders: List<FolderTracks>,
    onRemove: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onPreviewTrackClick: (folderUri: String, trackUri: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        items(items = folders, key = { it.folder.uri }) { folderTracks ->
            FolderListItem(
                folderTracks = folderTracks,
                onRemove = { onRemove(folderTracks.folder.uri) },
                onFolderClick = { onFolderClick(folderTracks.folder.uri) },
                onPreviewTrackClick = { trackUri -> onPreviewTrackClick(folderTracks.folder.uri, trackUri) },
            )
        }
    }
}
