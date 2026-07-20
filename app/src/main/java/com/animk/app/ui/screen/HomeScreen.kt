package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType
import com.animk.app.data.repository.MockDataRepository
import com.animk.app.ui.component.HeroBanner
import com.animk.app.ui.component.MediaRow
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun HomeScreen(
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    myList: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Anime", "Donghua", "Drakor")

    val featuredMedia = remember { MockDataRepository.heroFeatured }
    val animeList = remember { MockDataRepository.animeList }
    val donghuaList = remember { MockDataRepository.donghuaList }
    val drakorList = remember { MockDataRepository.drakorList }
    val top10List = remember { MockDataRepository.getAllMedia().filter { it.isTop10 } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Banner Header
            HeroBanner(
                media = featuredMedia,
                onPlayClick = onPlayClick,
                onDetailClick = onMediaClick,
                onToggleMyList = onToggleMyList,
                isInMyList = myList.any { it.id == featuredMedia.id }
            )

            // Content Rows based on category selection
            if (selectedCategory == "All" || selectedCategory == "Anime") {
                MediaRow(
                    title = "🔥 Trending Anime Now",
                    items = animeList,
                    onMediaClick = onMediaClick
                )
            }

            MediaRow(
                title = "🏆 Top 10 Today in Indonesia",
                items = top10List,
                onMediaClick = onMediaClick,
                isTop10Row = true
            )

            if (selectedCategory == "All" || selectedCategory == "Donghua") {
                MediaRow(
                    title = "⚔️ Popular Donghua & Cultivation",
                    items = donghuaList,
                    onMediaClick = onMediaClick
                )
            }

            if (selectedCategory == "All" || selectedCategory == "Drakor") {
                MediaRow(
                    title = "❤️ Korean Drama Hits",
                    items = drakorList,
                    onMediaClick = onMediaClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Top Overlay Header with Logo and Category Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AnimK",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = custom.primary,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    color = custom.surface.copy(alpha = 0.8f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "NETFLIX UI",
                        color = custom.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier.clickable { selectedCategory = category },
                        shape = CircleShape,
                        color = if (isSelected) custom.primary else custom.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) custom.onPrimary else custom.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
