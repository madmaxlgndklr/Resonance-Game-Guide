package com.madmaxlgndklr.resonanceguide

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.madmaxlgndklr.resonanceguide.navigation.BestiaryRoute
import com.madmaxlgndklr.resonanceguide.navigation.BuildsRoute
import com.madmaxlgndklr.resonanceguide.navigation.ImageViewerRoute
import com.madmaxlgndklr.resonanceguide.navigation.ItemsRoute
import com.madmaxlgndklr.resonanceguide.navigation.QuestDetailRoute
import com.madmaxlgndklr.resonanceguide.navigation.TopLevelTab
import com.madmaxlgndklr.resonanceguide.navigation.WalkthroughRoute
import com.madmaxlgndklr.resonanceguide.ui.bestiary.BestiaryScreen
import com.madmaxlgndklr.resonanceguide.ui.builds.BuildsScreen
import com.madmaxlgndklr.resonanceguide.ui.items.ItemsScreen
import com.madmaxlgndklr.resonanceguide.ui.theme.ResonanceTheme
import com.madmaxlgndklr.resonanceguide.ui.viewer.ImageViewerScreen
import com.madmaxlgndklr.resonanceguide.ui.walkthrough.QuestDetailScreen
import com.madmaxlgndklr.resonanceguide.ui.walkthrough.WalkthroughScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the status/navigation bars; screens apply insets themselves.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()

        setContent {
            ResonanceTheme {
                ResonanceGuideApp()
            }
        }
    }

    /**
     * Ask for the panel's fastest mode at the current resolution (120 Hz on the
     * S24 Ultra) so scrolling and zooming stay smooth even when the system's
     * adaptive refresh would otherwise pick a lower rate for this window.
     */
    private fun requestHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val display = display ?: return
        val current = display.mode
        val fastest = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = fastest.modeId }
    }
}

@Composable
fun ResonanceGuideApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Media screens go fully immersive, so the bottom bar steps aside.
    val showBottomBar = currentDestination?.hasRoute<ImageViewerRoute>() != true

    Scaffold(
        // Insets are handled per screen (top app bars) and by the NavigationBar,
        // so the viewer can draw under the hidden system bars.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ResonanceBottomBar(navController, currentDestination)
            }
        },
    ) { innerPadding ->
        ResonanceNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun ResonanceBottomBar(navController: NavHostController, currentDestination: NavDestination?) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Single back stack per tab: return to the start destination,
                        // keeping each tab's scroll position and nested screens.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
private fun ResonanceNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val openImage: (String, String) -> Unit = { path, title ->
        navController.navigate(ImageViewerRoute(assetPath = path, title = title))
    }

    NavHost(
        navController = navController,
        startDestination = WalkthroughRoute,
        modifier = modifier,
    ) {
        composable<WalkthroughRoute> {
            WalkthroughScreen(
                onQuestClick = { navController.navigate(QuestDetailRoute(it)) },
                onOpenMap = openImage,
            )
        }
        composable<QuestDetailRoute> {
            QuestDetailScreen(
                onBack = navController::navigateUp,
                onOpenMap = openImage,
            )
        }
        composable<BestiaryRoute> { BestiaryScreen(onOpenSprite = openImage) }
        composable<BuildsRoute> { BuildsScreen(onOpenSprite = openImage) }
        composable<ItemsRoute> { ItemsScreen() }
        composable<ImageViewerRoute> { entry ->
            val route = entry.toRoute<ImageViewerRoute>()
            ImageViewerScreen(
                assetPath = route.assetPath,
                title = route.title,
                onBack = navController::navigateUp,
            )
        }
    }
}
