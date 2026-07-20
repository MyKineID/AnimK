package com.animk.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
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
import com.animk.app.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    media: MediaItem,
    isInMyList: Boolean,
    onDismiss: () -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit
) {
    val custom = LocalCustomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = custom.surface,
        contentColor = custom.textPrimary,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                color = custom.textMuted.copy(alpha = 0.5f),
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header Image with Animated Close Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = media.backdropUrl.ifEmpty { media.posterUrl },
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Info Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = media.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${media.matchPercentage}% Match",
                            color = custom.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "${media.releaseYear}",
                            color = custom.textSecondary,
                            fontSize = 14.sp
                        )

                        Surface(
                            color = custom.cardSurface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = media.ageRating,
                                color = custom.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Animated Play Button
                    Button(
                        onClick = { onPlayClick(media) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = custom.primary,
                            contentColor = custom.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleMyList(media) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = custom.textPrimary)
                        ) {
                            Icon(
                                if (isInMyList) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (isInMyList) custom.primary else custom.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isInMyList) "In My List" else "My List")
                        }

                        OutlinedButton(
                            onClick = { /* Download action */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = custom.textPrimary)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = media.description,
                        color = custom.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Episodes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary
                    )
                }
            }

            // Episodes List
            items(media.episodes) { ep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayClick(media) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        AsyncImage(
                            model = ep.thumbnailUrl,
                            contentDescription = ep.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play Ep",
                                tint = custom.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ep.title,
                                fontWeight = FontWeight.SemiBold,
                                color = custom.textPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ep.duration,
                                color = custom.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ep.description,
                            color = custom.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
