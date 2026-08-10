package com.col.notnetmediaforge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.col.notnetmediaforge.ui.screens.HistoryScreen
import com.col.notnetmediaforge.ui.screens.HomeScreen
import com.col.notnetmediaforge.ui.screens.MediaDetailScreen
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onAnalyzed = { navController.navigate(Routes.DETAIL) }
            )
        }
        composable(Routes.DETAIL) {
            MediaDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel)
        }
    }
}
