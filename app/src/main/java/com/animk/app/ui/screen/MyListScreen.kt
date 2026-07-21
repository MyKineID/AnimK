package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
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
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = custom.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My List (${myList.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = custom.textPrimary
            )
        }

        if (myList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your list is empty. Add shows using '+ My List'",
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
                items(myList, key = { it.id }) { item ->
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
