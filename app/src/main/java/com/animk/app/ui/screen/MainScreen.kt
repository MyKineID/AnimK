package com.animk.app.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.animk.app.data.model.Episode
import com.animk.app.data.model.EpisodeSource
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.component.AuthSheet
import com.animk.app.ui.component.MediaDetailSheet
import com.animk.app.ui.theme.AppThemeAccent
import com.animk.app.ui.theme.LocalCustomColors

data class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomNavTab("Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab("Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavTab("My List", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavTab("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activeAccent: AppThemeAccent = AppThemeAccent.NEON_GECKO,
    onAccentChange: (AppThemeAccent) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val custom = LocalCustomColors.current

    var selectedMediaForDetail by remember { mutableStateOf<MediaItem?>(null) }
    // Now we track both the media AND the episode source URL for the player
    var playerMedia by remember { mutableStateOf<MediaItem?>(null) }
    var playerEpisodeUrl by remember { mutableStateOf("") }
    var playerEpisodeSources by remember { mutableStateOf<List<EpisodeSource>>(emptyList()) }
    var showAuthSheet by remember { mutableStateOf(false) }

    val myListState = remember { mutableStateListOf<MediaItem>() }

    fun toggleMyList(media: MediaItem) {
        val exists = myListState.any { it.id == media.id }
        if (exists) {
            myListState.removeAll { it.id == media.id }
        } else {
            myListState.add(media)
        }
    }

    if (playerMedia != null) {
        NetflixMediaPlayerScreen(
            media = playerMedia!!,
            episodeSourceUrl = playerEpisodeUrl,
            episodeSources = playerEpisodeSources,
            onBack = {
                // Back from player → return to detail sheet (reopen it)
                val backMedia = playerMedia
                playerMedia = null
                playerEpisodeUrl = ""
                playerEpisodeSources = emptyList()
                selectedMediaForDetail = backMedia
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = custom.surface,
                    contentColor = custom.textPrimary
                ) {
                    bottomTabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = custom.primary,
                                selectedTextColor = custom.primary,
                                unselectedIconColor = custom.textSecondary,
                                unselectedTextColor = custom.textSecondary,
                                indicatorColor = custom.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)

            when (selectedTabIndex) {
                0 -> HomeScreen(
                    onMediaClick = { selectedMediaForDetail = it },
                    onPlayClick = {
                        // AniList IDs are metadata IDs, not episode URLs. Open detail first
                        // so all matching provider episodes can be merged correctly.
                        selectedMediaForDetail = it
                    },
                    onResumeWatching = { entry ->
                        playerMedia = entry.media
                        playerEpisodeUrl = entry.episodeSourceUrl
                        playerEpisodeSources = listOf(EpisodeSource("", "", entry.episodeSourceUrl))
                    },
                    onToggleMyList = { toggleMyList(it) },
                    myList = myListState,
                    modifier = contentModifier
                )
                1 -> SearchScreen(
                    onMediaClick = { selectedMediaForDetail = it },
                    modifier = contentModifier
                )
                2 -> MyListScreen(
                    myList = myListState,
                    onMediaClick = { selectedMediaForDetail = it },
                    modifier = contentModifier
                )
                3 -> SettingsScreen(
                    activeAccent = activeAccent,
                    onAccentChange = onAccentChange,
                    onOpenAuthSheet = { showAuthSheet = true },
                    modifier = contentModifier
                )
            }

            // Detail Sheet - now passes Episode with real sourceUrl
            selectedMediaForDetail?.let { media ->
                MediaDetailSheet(
                    media = media,
                    isInMyList = myListState.any { it.id == media.id },
                    onDismiss = { selectedMediaForDetail = null },
                    onPlayEpisode = { mediaItem, episode ->
                        playerMedia = mediaItem
                        playerEpisodeUrl = episode.sourceUrl
                        playerEpisodeSources = episode.sources.ifEmpty {
                            listOf(EpisodeSource("", "", episode.sourceUrl))
                        }
                        selectedMediaForDetail = null
                    },
                    onToggleMyList = { toggleMyList(it) },
                    onRequireLogin = { showAuthSheet = true }
                )
            }

            // Supabase Auth Sheet
            if (showAuthSheet) {
                AuthSheet(
                    onDismiss = { showAuthSheet = false },
                    onAuthSuccess = { showAuthSheet = false }
                )
            }
        }
    }
}
