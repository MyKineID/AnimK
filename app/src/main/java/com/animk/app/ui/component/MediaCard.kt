package com.animk.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun MediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    val shape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .width(125.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = custom.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = media.posterUrl,
                contentDescription = media.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )

            // Top quality badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        color = custom.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = media.quality,
                    color = custom.onPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom title overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        color = Color.Black.copy(alpha = 0.75f)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = media.title,
                    color = custom.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
