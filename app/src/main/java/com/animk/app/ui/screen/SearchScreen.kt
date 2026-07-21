package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val aniListService = remember { AniListApiService() }
    val scraperRepo = remember { ScraperRepository() }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        scope.launch {
            try {
                val results = mutableListOf<MediaItem>()
                results.addAll(aniListService.searchAnime(query))
                if (results.isEmpty()) {
                    results.addAll(scraperRepo.searchAll(query))
                }
                searchResults = results.distinctBy { it.title }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearching = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.length >= 3) {
                    performSearch(it)
                }
            },
            placeholder = { Text("Search Anime, Donghua, Drakor...", color = custom.textMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = custom.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = emptyList()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = custom.textSecondary)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = custom.surface,
                unfocusedContainerColor = custom.surface,
                focusedBorderColor = custom.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = custom.textPrimary,
                unfocusedTextColor = custom.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = custom.primary)
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "Type to search for Anime, Donghua, or Drakor" else "No results found for '$searchQuery'",
                    color = custom.textMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier.clickable { onMediaClick(item) }
                    ) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            color = custom.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
