package com.animk.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun HeroBanner(
    media: MediaItem,
    onPlayClick: (MediaItem) -> Unit,
    onDetailClick: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    isInMyList: Boolean,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .clickable { onDetailClick(media) }
    ) {
        AsyncImage(
            model = media.backdropUrl.ifEmpty { media.posterUrl },
            contentDescription = media.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Scrim Gradients for Netflix effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            custom.background.copy(alpha = 0.8f),
                            custom.background
                        ),
                        startY = 0f,
                        endY = 1300f
                    )
                )
        )

        // Banner Content Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (media.isTop10) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "TOP 10",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = custom.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = media.quality,
                        color = custom.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${media.type.displayName} • ${media.releaseYear} • ${media.ageRating}",
                    color = custom.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = media.title,
                color = custom.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Genres
            Text(
                text = media.genres.joinToString(" • "),
                color = custom.textMuted,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                Button(
                    onClick = { onPlayClick(media) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = custom.primary,
                        contentColor = custom.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = custom.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // My List Button
                OutlinedButton(
                    onClick = { onToggleMyList(media) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = custom.textPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(custom.surface, custom.cardSurface))
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isInMyList) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "My List",
                        tint = if (isInMyList) custom.primary else custom.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInMyList) "In My List" else "My List",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
