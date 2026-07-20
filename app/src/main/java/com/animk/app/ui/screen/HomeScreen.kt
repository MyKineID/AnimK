package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Whatshot
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

    val carouselMediaList = remember { MockDataRepository.getAllMedia().take(4) }
    val animeList = remember { MockDataRepository.animeList }
    val donghuaList = remember { MockDataRepository.donghuaList }
    val drakorList = remember { MockDataRepository.drakorList }
    val top10List = remember { MockDataRepository.getAllMedia().filter { it.isTop10 } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
    ) {
        // High performance LazyColumn for smooth 60-120Hz scrolling
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Multi-item Carousel Banner
            item {
                HeroBanner(
                    items = carouselMediaList,
                    onPlayClick = onPlayClick,
                    onDetailClick = onMediaClick,
                    onToggleMyList = onToggleMyList,
                    myList = myList
                )
            }

            // Anime Row
            if (selectedCategory == "All" || selectedCategory == "Anime") {
                item {
                    MediaRow(
                        title = "Trending Anime Now",
                        icon = Icons.Filled.Whatshot,
                        items = animeList,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Top 10 Row
            item {
                MediaRow(
                    title = "Top 10 Today in Indonesia",
                    icon = Icons.Filled.EmojiEvents,
                    items = top10List,
                    onMediaClick = onMediaClick,
                    isTop10Row = true
                )
            }

            // Donghua Row
            if (selectedCategory == "All" || selectedCategory == "Donghua") {
                item {
                    MediaRow(
                        title = "Popular Donghua & Cultivation",
                        icon = Icons.Filled.FlashOn,
                        items = donghuaList,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Drakor Row
            if (selectedCategory == "All" || selectedCategory == "Drakor") {
                item {
                    MediaRow(
                        title = "Korean Drama Hits",
                        icon = Icons.Filled.Favorite,
                        items = drakorList,
                        onMediaClick = onMediaClick
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Top Overlay Header with Logo and Category Filter Chips (Clean logo without "NETFLIX UI" text)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AnimK",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = custom.primary,
                    letterSpacing = 0.5.sp
                )
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
