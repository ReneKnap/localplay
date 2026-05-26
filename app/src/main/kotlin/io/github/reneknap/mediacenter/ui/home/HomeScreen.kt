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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.reneknap.mediacenter.R
import io.github.reneknap.mediacenter.data.theme.ThemeMode
import io.github.reneknap.mediacenter.ui.components.StatusMessage

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onFolderClick: (String) -> Unit,
    onEntryClick: (folderUri: String, entryUri: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    supportHintViewModel: SupportHintViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSupportHint by supportHintViewModel.showSupportHint.collectAsStateWithLifecycle()

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let { viewModel.addFolder(it.toString()) }
        }

    HomeContent(
        uiState = uiState,
        themeMode = themeMode,
        showSupportHint = showSupportHint,
        onToggleTheme = onToggleTheme,
        onSupportMenuOpened = supportHintViewModel::onSupportMenuOpened,
        onPickFolder = { pickerLauncher.launch(null) },
        onRemoveFolder = viewModel::removeFolder,
        onFolderClick = onFolderClick,
        onEntryClick = onEntryClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    themeMode: ThemeMode,
    showSupportHint: Boolean,
    onToggleTheme: () -> Unit,
    onSupportMenuOpened: () -> Unit,
    onPickFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onEntryClick: (folderUri: String, entryUri: String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    ThemeToggleButton(themeMode = themeMode, onToggleTheme = onToggleTheme)
                    HomeOverflowMenu(
                        expanded = menuExpanded,
                        showBadge = showSupportHint,
                        onExpandedChange = { expanded ->
                            menuExpanded = expanded
                            if (expanded) {
                                onSupportMenuOpened()
                            }
                        },
                        onSupportClick = {
                            menuExpanded = false
                            showSupportDialog = true
                        },
                    )
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
                        onEntryClick = onEntryClick,
                    )
            }
        }
        if (showSupportDialog) {
            SupportDialog(onDismiss = { showSupportDialog = false })
        }
    }
}

@Composable
private fun HomeOverflowMenu(
    expanded: Boolean,
    showBadge: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSupportClick: () -> Unit,
) {
    Box {
        BadgedBox(
            badge = {
                if (showBadge) {
                    Badge()
                }
            },
        ) {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.home_more_options),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.support_menu_item)) },
                onClick = onSupportClick,
            )
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
    folders: List<FolderMediaUi>,
    onRemove: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onEntryClick: (folderUri: String, entryUri: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        items(items = folders, key = { it.folder.uri }) { folderMedia ->
            FolderListItem(
                folderMedia = folderMedia,
                onRemove = { onRemove(folderMedia.folder.uri) },
                onFolderClick = { onFolderClick(folderMedia.folder.uri) },
                onEntryClick = { entryUri -> onEntryClick(folderMedia.folder.uri, entryUri) },
            )
        }
    }
}
