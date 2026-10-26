package com.madmaxlgndklr.resonanceguide.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.ui.graphics.vector.ImageVector
import com.madmaxlgndklr.resonanceguide.R
import kotlinx.serialization.Serializable

// Type-safe Navigation Compose destinations.

@Serializable data object WalkthroughRoute
@Serializable data object BestiaryRoute
@Serializable data object BuildsRoute
@Serializable data object ItemsRoute

/** @param chapterId a dataset chapter id, e.g. `ch_01_chapter_1_tailing_the_truth`. */
@Serializable data class ChapterDetailRoute(val chapterId: String)

/** Full-screen, immersive viewer for a map or sprite. */
@Serializable data class ImageViewerRoute(val assetPath: String, val title: String)

enum class TopLevelTab(
    val route: Any,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Walkthrough(WalkthroughRoute, R.string.tab_walkthrough, Icons.Filled.Explore, Icons.Outlined.Explore),
    Bestiary(BestiaryRoute, R.string.tab_bestiary, Icons.Filled.Pets, Icons.Outlined.Pets),
    Builds(BuildsRoute, R.string.tab_builds, Icons.Filled.Person, Icons.Outlined.Person),
    Items(ItemsRoute, R.string.tab_items, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
}
