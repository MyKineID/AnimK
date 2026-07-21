package com.animk.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun HeroBanner(
    items: List<MediaItem>,
    isInMyList: (MediaItem) -> Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return
    val custom = LocalCustomColors.current
    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = items[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMediaClick(media) }
            ) {
                AsyncImage(
                    model = media.backdropUrl ?: media.posterUrl,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top & Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    custom.background
                                ),
                                startY = 0f,
                                endY = 1200f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = media.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Genres Tag
                    Text(
                        text = media.genres.take(3).joinToString(" • "),
                        fontSize = 12.sp,
                        color = custom.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // My List Button
                        OutlinedButton(
                            onClick = { onToggleMyList(media) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = custom.textPrimary),
                            modifier = Modifier.height(40.dp)
                        ) {
                            val inList = isInMyList(media)
                            Icon(
                                if (inList) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (inList) custom.primary else custom.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (inList) "In My List" else "My List", fontSize = 13.sp)
                        }

                        // Play Button
                        Button(
                            onClick = { onPlayClick(media) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = custom.primary,
                                contentColor = custom.onPrimary
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Pager Indicator Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) custom.primary else Color.White.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
