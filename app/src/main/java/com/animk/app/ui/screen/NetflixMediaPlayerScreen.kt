package com.animk.app.ui.screen

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
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
import com.animk.app.data.model.StreamData
import com.animk.app.data.scraper.AnimeOrchestrator
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val orchestrator = remember { AnimeOrchestrator() }
    var availableStreams by remember { mutableStateOf<List<StreamData>>(emptyList()) }
    var activeStream by remember { mutableStateOf<StreamData?>(null) }
    var isFetchingStreams by remember { mutableStateOf(true) }

    LaunchedEffect(media.title) {
        scope.launch {
            val streams = orchestrator.getStreamsWaterfall(media.title)
            availableStreams = streams
            activeStream = streams.firstOrNull()
            isFetchingStreams = false
        }
    }

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

    // Double tap feedback overlays
    var showLeftDoubleTapBadge by remember { mutableStateOf(false) }
    var showRightDoubleTapBadge by remember { mutableStateOf(false) }

    // Drag Y offset for Swipe Down Exit animation
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val swipeProgress = (dragOffsetY.coerceIn(0f, 220f) / 220f)
    val contentAlpha = 1f - (swipeProgress * 0.7f)
    val contentScale = 1f - (swipeProgress * 0.1f)

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
            .background(Color.Transparent)
    ) {
        // Behind-video backdrop (visible during swipe down)
        AsyncImage(
            model = media.backdropUrl ?: media.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.4f),
            contentScale = ContentScale.Crop
        )

        // Actual player container that moves with swipe
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffsetY.coerceAtLeast(0f)
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                }
                .background(Color.Black, RoundedCornerShape((swipeProgress * 24).dp))
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
                model = media.backdropUrl ?: media.posterUrl,
                contentDescription = media.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = when (aspectRatioMode) {
                    "Fill" -> ContentScale.FillBounds
                    "Zoom" -> ContentScale.Crop
                    else -> ContentScale.Fit
                }
            )

            // Subtle gradient overlay when controls visible
            if (isControlsVisible && !isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            // Top 2x Speed Indicator Pill
            if (is2xSpeedActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = custom.primary.copy(alpha = 0.85f),
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
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
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
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Floating Lock Button when Locked
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
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

            // Animated Controls Overlay
            AnimatedVisibility(
                visible = isControlsVisible && !isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    text = activeStream?.serverName ?: "Server: Auto Stream",
                                    color = custom.primary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSpeedSheet = true }) {
                                Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White.copy(alpha = 0.85f))
                            }
                            IconButton(onClick = {
                                aspectRatioMode = when (aspectRatioMode) {
                                    "Fit" -> "Fill"
                                    "Fill" -> "Zoom"
                                    else -> "Fit"
                                }
                            }) {
                                Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White.copy(alpha = 0.85f))
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
                            onClick = { currentPosition = (currentPosition - 10f).coerceAtLeast(0f) },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Filled.FastRewind, contentDescription = "Rewind 10s", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
                        }

                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(68.dp)
                                .background(custom.primary.copy(alpha = 0.55f), CircleShape)
                        ) {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f))
                                        .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f))
                                },
                                label = "PlayPauseAnim"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { currentPosition = (currentPosition + 10f).coerceAtMost(totalDuration) },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Filled.FastForward, contentDescription = "Forward 10s", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
                        }
                    }

                    // Bottom Bar & Slider
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                )
                            )

                            Text(
                                text = String.format("%02d:00", totalMinutes),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Filled.LockOpen, contentDescription = "Lock Screen", tint = Color.White.copy(alpha = 0.7f))
                            }

                            TextButton(onClick = { showEpisodeSheet = true }) {
                                Icon(Icons.Filled.VideoLibrary, contentDescription = "Episodes", tint = custom.primary.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Episodes", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            }
                        }
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
