package com.animk.app.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.animk.app.ui.theme.LocalCustomColors

data class BottomTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

val tabs = listOf(
    BottomTab("Anime", Icons.Filled.Tv, Icons.Outlined.Tv),
    BottomTab("Donghua", Icons.Filled.Movie, Icons.Outlined.Movie),
    BottomTab("Drakor", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val custom = LocalCustomColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AnimK", color = custom.primary)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = custom.surface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = custom.primary,
                            selectedTextColor = custom.primary,
                            unselectedIconColor = custom.textSecondary,
                            unselectedTextColor = custom.textSecondary,
                            indicatorColor = custom.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> AnimeList(modifier = Modifier.padding(innerPadding))
            1 -> DonghuaList(modifier = Modifier.padding(innerPadding))
            2 -> DrakorList(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun AnimeList(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        Text("Anime", color = LocalCustomColors.current.textSecondary)
    }
}

@Composable
private fun DonghuaList(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        Text("Donghua", color = LocalCustomColors.current.textSecondary)
    }
}

@Composable
private fun DrakorList(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        Text("Drakor", color = LocalCustomColors.current.textSecondary)
    }
}
