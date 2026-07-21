package com.animk.app.ui.screen

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

data class CommentItem(
    val id: String,
    val author: String,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 12
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Immersive Fullscreen Mode + Landscape Lock
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = previousOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableFloatStateOf(120f) }
    val totalDuration = 1440f
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var is2xSpeedActive by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf("Fit") }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    // Interactive Social Stats State
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(14820) }
    val viewsCount = "1.2M Views"

    // Comments list state
    val comments = remember {
        mutableStateListOf(
            CommentItem("c1", "OtakuKing99", "Solo leveling animation in this episode is insane! 🔥", "2m ago", 45),
            CommentItem("c2", "KineFan_ID", "Sung Jinwoo leveling up sequence was epic! 10/10", "15m ago", 28),
            CommentItem("c3", "AnimeLover2024", "Can't wait for episode 2 next week!", "1h ago", 14)
        )
    }
    var newCommentText by remember { mutableStateOf("") }

    // Double tap feedback overlays
    var showLeftDoubleTapBadge by remember { mutableStateOf(false) }
    var showRightDoubleTapBadge by remember { mutableStateOf(false) }

    // Drag Y offset for Swipe Down Exit animation
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showLeftDoubleTapBadge) {
        if (showLeftDoubleTapBadge) {
            delay(800)
            showLeftDoubleTapBadge = false
        }
    }

    LaunchedEffect(showRightDoubleTapBadge) {
        if (showRightDoubleTapBadge) {
            delay(800)
            showRightDoubleTapBadge = false
        }
    }

    LaunchedEffect(isPlaying, isControlsVisible, isLocked) {
        if (isPlaying && isControlsVisible && !isLocked) {
            delay(4000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dragOffsetY.coerceAtLeast(0f)
            }
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        if (dragOffsetY > 220f && !isLocked) {
                            onBack()
                        }
                    },
                    onDragEnd = { dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f }
                )
            }
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        if (!isLocked) {
                            isControlsVisible = !isControlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                currentPosition = (currentPosition - 10f).coerceAtLeast(0f)
                                showLeftDoubleTapBadge = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                currentPosition = (currentPosition + 10f).coerceAtMost(totalDuration)
                                showRightDoubleTapBadge = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onPress = {
                        if (!isLocked) {
                            tryAwaitRelease()
                            if (is2xSpeedActive) {
                                is2xSpeedActive = false
                            }
                        }
                    },
                    onLongPress = {
                        if (!isLocked) {
                            is2xSpeedActive = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
    ) {
        // Fullscreen Video Backdrop
        AsyncImage(
            model = media.backdropUrl.ifEmpty { media.posterUrl },
            contentDescription = media.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = when (aspectRatioMode) {
                "Fill" -> ContentScale.FillBounds
                "Zoom" -> ContentScale.Crop
                else -> ContentScale.Fit
            }
        )

        // Overlay Dimming
        if (isControlsVisible && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Top 2x Speed Indicator Pill
        if (is2xSpeedActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = custom.primary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FastForward, contentDescription = null, tint = custom.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2X Speed Active",
                            color = custom.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Double Tap Badges
        if (showLeftDoubleTapBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .size(90.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (showRightDoubleTapBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .size(90.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Single Floating Lock Button when Locked
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Unlock Screen",
                        tint = custom.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Animated Full Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${media.episodes.firstOrNull()?.title ?: "Episode 1"} • $viewsCount",
                                color = custom.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Like Button
                        IconButton(onClick = {
                            isLiked = !isLiked
                            if (isLiked) {
                                isDisliked = false
                                likeCount++
                            } else {
                                likeCount--
                            }
                        }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLiked) custom.primary else Color.White
                            )
                        }
                        Text(
                            text = "$likeCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dislike Button
                        IconButton(onClick = {
                            isDisliked = !isDisliked
                            if (isDisliked && isLiked) {
                                isLiked = false
                                likeCount--
                            }
                        }) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) Color.Red else Color.White
                            )
                        }

                        // Comments Button
                        IconButton(onClick = { showCommentsSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.Comment,
                                contentDescription = "Comments",
                                tint = custom.primary
                            )
                        }

                        IconButton(onClick = { showSpeedSheet = true }) {
                            Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White)
                        }

                        IconButton(onClick = {
                            aspectRatioMode = when (aspectRatioMode) {
                                "Fit" -> "Fill"
                                "Fill" -> "Zoom"
                                else -> "Fit"
                            }
                        }) {
                            Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentPosition = (currentPosition - 10f).coerceAtLeast(0f)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Smooth Animated Play/Pause Button
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(72.dp)
                            .background(custom.primary, CircleShape)
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "PlayPauseAnim"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = custom.onPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            currentPosition = (currentPosition + 10f).coerceAtMost(totalDuration)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Controls Bar & Slider
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentMinutes = (currentPosition / 60).toInt()
                        val currentSecs = (currentPosition % 60).toInt()
                        val totalMinutes = (totalDuration / 60).toInt()

                        Text(
                            text = String.format("%02d:%02d", currentMinutes, currentSecs),
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Slider(
                            value = currentPosition,
                            onValueChange = { currentPosition = it },
                            valueRange = 0f..totalDuration,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = custom.primary,
                                activeTrackColor = custom.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = String.format("%02d:00", totalMinutes),
                            color = custom.textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isLocked = true }) {
                            Icon(Icons.Filled.LockOpen, contentDescription = "Lock Screen", tint = Color.White)
                        }

                        TextButton(onClick = { showEpisodeSheet = true }) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = "Episodes", tint = custom.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Episodes", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { showCommentsSheet = true }) {
                            Icon(Icons.Filled.Comment, contentDescription = "Comments", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Comments (${comments.size})", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Comments Sheet
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
                        .heightIn(max = 240.dp)
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
                                Text(c.text, color = custom.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post New Comment Bar
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
            }
        }
    }

    // Speed Selection Modal Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = custom.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Playback Speed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playbackSpeed = speed
                                showSpeedSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                            color = if (playbackSpeed == speed) custom.primary else custom.textPrimary,
                            fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                        )
                        if (playbackSpeed == speed) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = custom.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Episodes Selector Sheet
    if (showEpisodeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSheet = false },
            containerColor = custom.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Episode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                media.episodes.forEach { ep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEpisodeSheet = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = custom.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(ep.title, color = custom.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(ep.duration, color = custom.textMuted, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
