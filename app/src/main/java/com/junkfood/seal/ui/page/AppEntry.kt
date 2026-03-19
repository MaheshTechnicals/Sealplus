package com.junkfood.seal.ui.page

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.navigation.NavigationActions
import com.junkfood.seal.ui.navigation.drawer.NavigationDrawer
import com.junkfood.seal.ui.navigation.graph.HomeNavGraph
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.settings.network.CookiesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppEntry(dialogViewModel: DownloadDialogViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val cookiesViewModel: CookiesViewModel = koinViewModel()


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = remember(navBackStackEntry) {
        derivedStateOf { navBackStackEntry?.destination?.route }
    }.value

    val actions = remember(navController) { NavigationActions(navController) }

    LaunchedEffect(dialogViewModel.sheetStateFlow) {
        dialogViewModel.sheetStateFlow.collect { state ->
            if (state is DownloadDialogViewModel.SheetState.Configure) {
                if (navController.currentDestination?.route != Route.HOME) {
                    navController.popBackStack(Route.HOME, false, saveState = true)
                }
            }
        }
    }

    val isHome = remember(currentRoute) { currentRoute == Route.HOME }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val windowWidth = LocalWindowWidthState.current


    NavigationDrawer(
        windowWidth = windowWidth,
        drawerState = drawerState,
        currentRoute = currentRoute,
        currentTopDestination = Route.HOME,
        showQuickSettings = true,
        gesturesEnabled = isHome,
        onDismissRequest = { scope.launch { drawerState.close() } },
        onNavigateToRoute = { route ->
            actions.navigateTo(route)
            scope.launch { drawerState.close() }
        }
    ) {
        HomeNavGraph(
            navController = navController,
            dialogViewModel = dialogViewModel,
            onMenuOpen = {
                view.slightHapticFeedback()
                scope.launch { drawerState.open() }
            },
            onNavigateBack = actions.onBack,
            cookiesViewModel = cookiesViewModel
        )

        AppUpdater()
        YtdlpUpdater()
    }

}

