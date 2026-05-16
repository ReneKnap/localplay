package io.github.reneknap.mediacenter.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.reneknap.mediacenter.ui.home.HomeScreen

private const val ROUTE_HOME = "home"

@Composable
fun MediaCenterNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
    ) {
        composable(ROUTE_HOME) {
            HomeScreen()
        }
    }
}
