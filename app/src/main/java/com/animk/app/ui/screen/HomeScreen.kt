package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
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
import com.animk.app.data.playback.WatchHistoryEntry
import com.animk.app.data.playback.WatchHistoryStore
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.ui.component.HeroBanner
import com.animk.app.ui.component.MediaRow
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onResumeWatching: (WatchHistoryEntry) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    myList: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    val scope = rememberCoroutineScope()

    val aniListService = remember { AniListApiService() }
    val scraperRepo = remember { ScraperRepository() }

    var ongoingAnime by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var trendingAnime by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var donghuaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var drakorList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var drachinList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var japaneseDramaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<WatchHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            continueWatching = WatchHistoryStore.history()
            try {
                // Render the first screen as soon as its two essential requests finish.
                val (ongoing, aniTrending) = coroutineScope {
                    val ongoingRequest = async { scraperRepo.fetchOngoing(limit = 50) }
                    val trendingRequest = async { aniListService.getTrendingAnime(page = 1, perPage = 50) }
                    ongoingRequest.await() to trendingRequest.await()
                }
                ongoingAnime = ongoing.filter { it.type == MediaType.ANIME }.distinctBy { it.title }
                trendingAnime = aniTrending
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }

            // These sections do not block the home screen or its first interaction.
            launch { donghuaList = scraperRepo.searchByType("", MediaType.DONGHUA).take(20) }
            launch { drakorList = scraperRepo.searchByType("", MediaType.DRAKOR).take(20) }
            launch { drachinList = scraperRepo.searchByType("", MediaType.DRACHIN).take(20) }
            launch { japaneseDramaList = scraperRepo.searchByType("", MediaType.JDRAMA).take(20) }
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
            // Brand Header
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

            // Hero Banner Carousel (from AniList trending)
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

            // Resume the exact episode and saved position.
            item {
                val historyItems = continueWatching.map { entry ->
                    entry.media.copy(id = "${entry.media.id}#history#${entry.episodeSourceUrl}")
                }
                if (historyItems.isNotEmpty()) {
                    MediaRow(
                        title = "Continue Watching",
                        icon = Icons.Filled.History,
                        items = historyItems,
                        onMediaClick = { item ->
                            continueWatching.firstOrNull { "${it.media.id}#history#${it.episodeSourceUrl}" == item.id }
                                ?.let(onResumeWatching)
                        },
                        collapsedCount = 6
                    )
                }
            }

            // 🔥 Ongoing Anime - From Otakudesu (primary scraper)
            item {
                if (ongoingAnime.isNotEmpty()) {
                    MediaRow(
                        title = "Ongoing Anime",
                        icon = Icons.Filled.PlayCircle,
                        items = ongoingAnime,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            // AniList brings a wider discovery catalog; provider matching happens
            // when the detail sheet opens, so this screen is never artificially sparse.
            item {
                if (trendingAnime.take(16).isNotEmpty()) {
                    MediaRow(
                        title = "Trending Now",
                        icon = Icons.Filled.Whatshot,
                        items = trendingAnime.take(16),
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            item {
                if (trendingAnime.drop(16).take(18).isNotEmpty()) {
                    MediaRow(
                        title = "Popular Anime",
                        icon = Icons.Filled.EmojiEvents,
                        items = trendingAnime.drop(16).take(18),
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            item {
                if (trendingAnime.drop(34).isNotEmpty()) {
                    MediaRow(
                        title = "Pilihan Untukmu",
                        icon = Icons.Filled.Favorite,
                        items = trendingAnime.drop(34),
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            // Anime List (extended from all active scrapers + AniList)
            item {
                val mixedAnime = (ongoingAnime + trendingAnime)
                    .filter { it.type == MediaType.ANIME }
                    .distinctBy { it.title.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim() }
                if (mixedAnime.size > 6) {
                    MediaRow(
                        title = "Jelajahi Semua Anime",
                        icon = Icons.Filled.Movie,
                        items = mixedAnime,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            // 3D Donghua Section
            item {
                if (donghuaList.isNotEmpty()) {
                    MediaRow(
                        title = "3D Donghua",
                        icon = Icons.Filled.FlashOn,
                        items = donghuaList,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            // Korean Drama Section
            item {
                if (drakorList.isNotEmpty()) {
                    MediaRow(
                        title = "Korean Drama",
                        icon = Icons.Filled.Favorite,
                        items = drakorList,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            item {
                if (drachinList.isNotEmpty()) {
                    MediaRow(
                        title = "Chinese Drama",
                        icon = Icons.Filled.Movie,
                        items = drachinList,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            item {
                if (japaneseDramaList.isNotEmpty()) {
                    MediaRow(
                        title = "Japanese Drama",
                        icon = Icons.Filled.Favorite,
                        items = japaneseDramaList,
                        onMediaClick = onMediaClick,
                        collapsedCount = 6
                    )
                }
            }

            // Spacer for bottom nav
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
