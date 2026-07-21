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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
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

data class CommentItem(
    val id: String,
    val author: String,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 12
)

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

    // Social interaction state
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(14820) }
    var dislikeCount by remember { mutableIntStateOf(342) }
    val viewsCount = "1.2M"
    var showCommentsSheet by remember { mutableStateOf(false) }

    // Comments state
    val comments = remember {
        mutableStateListOf(
            CommentItem("c1", "OtakuKing99", "Solo leveling animation in this episode is insane! 🔥", "2m ago", 45),
            CommentItem("c2", "KineFan_ID", "Sung Jinwoo leveling up sequence was epic! 10/10", "15m ago", 28),
            CommentItem("c3", "AnimeLover2024", "Can't wait for episode 2 next week!", "1h ago", 14)
        )
    }
    var newCommentText by remember { mutableStateOf("") }

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
            // Header Image with Close Button
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

                    // Big Play Button
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // ========== Social Interaction Bar ==========
                    Surface(
                        color = custom.cardSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Views
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = "Views",
                                    tint = custom.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(viewsCount, color = custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Like
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isLiked = !isLiked
                                        if (isLiked) {
                                            isDisliked = false
                                            likeCount++
                                        } else {
                                            likeCount--
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (isLiked) custom.primary else custom.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCount(likeCount),
                                    color = if (isLiked) custom.primary else custom.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dislike
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isDisliked = !isDisliked
                                        if (isDisliked && isLiked) {
                                            isLiked = false
                                            likeCount--
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                    contentDescription = "Dislike",
                                    tint = if (isDisliked) Color(0xFFEF5350) else custom.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCount(dislikeCount),
                                    color = if (isDisliked) Color(0xFFEF5350) else custom.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Comments
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCommentsSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ChatBubbleOutline,
                                    contentDescription = "Comments",
                                    tint = custom.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${comments.size}", color = custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

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

    // Comments Bottom Sheet
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            containerColor = custom.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Comments (${comments.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                    IconButton(onClick = { showCommentsSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = custom.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(custom.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c.author.take(1), fontWeight = FontWeight.Bold, color = custom.onPrimary, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.author, fontWeight = FontWeight.Bold, color = custom.textPrimary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(c.timestamp, color = custom.textMuted, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(c.text, color = custom.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post New Comment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...", color = custom.textMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = custom.cardSurface,
                            unfocusedContainerColor = custom.cardSurface,
                            focusedBorderColor = custom.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = custom.textPrimary,
                            unfocusedTextColor = custom.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                comments.add(
                                    0,
                                    CommentItem(
                                        id = "c_${System.currentTimeMillis()}",
                                        author = "AnimK User",
                                        text = newCommentText.trim(),
                                        timestamp = "Just now"
                                    )
                                )
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier
                            .background(custom.primary, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = custom.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> "$count"
    }
}
