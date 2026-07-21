package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.data.network.AniListApiService
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val aniListService = remember { AniListApiService() }
    val scraperRepo = remember { ScraperRepository() }
    val quickQueries = remember { listOf("Action", "Isekai", "Romance", "Donghua", "K-Drama") }

    LaunchedEffect(Unit) {
        suggestions = aniListService.getTrendingAnime(page = 1, perPage = 18)
    }

    // Debounce prevents a request per keystroke and combines catalog + providers.
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        if (query.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        delay(350)
        isSearching = true
        try {
            val merged = coroutineScope {
                val aniList = async { aniListService.searchAnime(query) }
                val providers = async { scraperRepo.searchAll(query) }
                aniList.await() + providers.await()
            }
            searchResults = merged.distinctBy(::searchKey)
        } catch (e: Exception) {
            e.printStackTrace()
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Cari tontonan", color = custom.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Anime, donghua, K-drama, C-drama, dan J-drama dari semua sumber", color = custom.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Judul anime atau drama", color = custom.textMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = custom.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian", tint = custom.textSecondary)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = custom.cardSurface,
                unfocusedContainerColor = custom.cardSurface,
                focusedBorderColor = custom.primary.copy(alpha = 0.8f),
                unfocusedBorderColor = custom.cardSurface,
                focusedTextColor = custom.textPrimary,
                unfocusedTextColor = custom.textPrimary,
                cursorColor = custom.primary
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        when {
            isSearching -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = custom.primary)
            }
            searchQuery.isNotBlank() && searchResults.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.TravelExplore, contentDescription = null, tint = custom.textMuted, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Tidak menemukan \"${searchQuery.trim()}\"", color = custom.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Coba judul lain atau gunakan kata yang lebih singkat.", color = custom.textMuted, fontSize = 12.sp)
                }
            }
            else -> {
                val showingSuggestions = searchQuery.isBlank()
                val itemsToShow = if (showingSuggestions) suggestions else searchResults
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showingSuggestions) {
                        item(span = { GridItemSpan(3) }) {
                            Column {
                                Text("Mulai dari sini", color = custom.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(7.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                                    quickQueries.take(3).forEach { query ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { searchQuery = query },
                                            label = { Text(query, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = custom.cardSurface,
                                                labelColor = custom.textSecondary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = Color.Transparent,
                                                selectedBorderColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("Populer sekarang", color = custom.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                    items(itemsToShow, key = { "${it.id}:${it.title}" }) { item ->
                        Column(modifier = Modifier.clickable { onMediaClick(item) }) {
                            AsyncImage(
                                model = item.posterUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = item.title,
                                color = custom.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                minLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun searchKey(item: MediaItem): String = item.title.lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
