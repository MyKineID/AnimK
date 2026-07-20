package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.model.MediaItem
import com.animk.app.data.repository.MockDataRepository
import com.animk.app.ui.component.MediaCard
import com.animk.app.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    val genres = listOf("All", "Action", "Romance", "Fantasy", "Drama", "Shounen", "Supernatural", "Thriller")

    val allMedia = remember { MockDataRepository.getAllMedia() }

    val filteredMedia = remember(searchQuery, selectedGenre) {
        allMedia.filter { item ->
            val matchesQuery = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenre == "All" || item.genres.contains(selectedGenre)
            matchesQuery && matchesGenre
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .padding(top = 8.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search Anime, Donghua, Drakor...", color = custom.textMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = custom.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = custom.textSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = custom.surface,
                unfocusedContainerColor = custom.surface,
                focusedBorderColor = custom.primary,
                unfocusedBorderColor = custom.cardSurface,
                focusedTextColor = custom.textPrimary,
                unfocusedTextColor = custom.textPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Genre Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGenre = genre },
                    label = {
                        Text(
                            text = genre,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = custom.primary,
                        selectedLabelColor = custom.onPrimary,
                        containerColor = custom.surface,
                        labelColor = custom.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = custom.cardSurface,
                        selectedBorderColor = custom.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isEmpty()) "Top Searches & Recommendations" else "Search Results (${filteredMedia.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = custom.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Results Grid
        if (filteredMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No content found matching \"$searchQuery\"",
                    color = custom.textMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMedia, key = { it.id }) { item ->
                    MediaCard(
                        media = item,
                        onClick = { onMediaClick(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
