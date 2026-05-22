package io.github.reneknap.mediacenter.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.reneknap.mediacenter.data.theme.ThemeMode
import io.github.reneknap.mediacenter.ui.home.HomeScreen
import io.github.reneknap.mediacenter.ui.player.FolderPlayerScreen

private const val ROUTE_HOME = "home"
private const val ARG_FOLDER_URI = "folderUri"
private const val ARG_START_TRACK_URI = "startTrackUri"
private const val ROUTE_FOLDER = "folder/{$ARG_FOLDER_URI}?$ARG_START_TRACK_URI={$ARG_START_TRACK_URI}"

@Composable
fun MediaCenterNavGraph(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
                onFolderClick = { folderUri ->
                    navController.navigate("folder/${Uri.encode(folderUri)}")
                },
                onPreviewTrackClick = { folderUri, trackUri ->
                    navController.navigate(
                        "folder/${Uri.encode(folderUri)}?$ARG_START_TRACK_URI=${Uri.encode(trackUri)}",
                    )
                },
            )
        }
        composable(
            route = ROUTE_FOLDER,
            arguments =
                listOf(
                    navArgument(ARG_FOLDER_URI) { type = NavType.StringType },
                    navArgument(ARG_START_TRACK_URI) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) {
            FolderPlayerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
