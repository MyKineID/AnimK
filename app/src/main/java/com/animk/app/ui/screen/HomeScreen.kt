package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType
import com.animk.app.data.network.AniListApiService
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.ui.component.HeroBanner
import com.animk.app.ui.component.MediaRow
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    myList: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    val scope = rememberCoroutineScope()

    val aniListService = remember { AniListApiService() }
    val scraperRepo = remember { ScraperRepository() }

    var trendingAnime by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var popularAnime by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var donghuaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var drakorList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                trendingAnime = aniListService.getTrendingAnime(page = 1, perPage = 6)
                popularAnime = aniListService.searchAnime("Naruto")
                donghuaList = scraperRepo.searchByType("BTTH", MediaType.DONGHUA)
                drakorList = scraperRepo.searchByType("Love", MediaType.DRAKOR)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Clean Brand Header Logo
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = "AnimK Logo",
                        tint = custom.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AnimK",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.primary
                    )
                }
            }

            // Hero Banner Carousel
            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = custom.primary)
                    }
                } else if (trendingAnime.isNotEmpty()) {
                    HeroBanner(
                        items = trendingAnime,
                        isInMyList = { item -> myList.any { it.id == item.id } },
                        onMediaClick = onMediaClick,
                        onPlayClick = onPlayClick,
                        onToggleMyList = onToggleMyList
                    )
                }
            }

            // Trending Anime Section
            item {
                if (trendingAnime.isNotEmpty()) {
                    MediaRow(
                        title = "Trending Now",
                        icon = Icons.Filled.Whatshot,
                        items = trendingAnime,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Popular Anime Section
            item {
                if (popularAnime.isNotEmpty()) {
                    MediaRow(
                        title = "Popular Anime",
                        icon = Icons.Filled.EmojiEvents,
                        items = popularAnime,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // 3D Donghua Section (from remote config)
            item {
                if (donghuaList.isNotEmpty()) {
                    MediaRow(
                        title = "3D Donghua",
                        icon = Icons.Filled.FlashOn,
                        items = donghuaList,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Korean Drama Section (from remote config)
            item {
                if (drakorList.isNotEmpty()) {
                    MediaRow(
                        title = "Korean Drama",
                        icon = Icons.Filled.Favorite,
                        items = drakorList,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }
    }
}
