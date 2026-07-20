package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.component.MediaCard
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun MyListScreen(
    myList: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My List",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = custom.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${myList.size} Saved)",
                fontSize = 14.sp,
                color = custom.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (myList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = custom.textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap '+ My List' on any Anime, Donghua, or Drakor to save it here for quick access.",
                        fontSize = 13.sp,
                        color = custom.textMuted,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(myList, key = { it.id }) { item ->
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
