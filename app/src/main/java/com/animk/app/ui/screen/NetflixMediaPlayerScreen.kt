package com.animk.app.ui.screen

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Lock screen orientation to Landscape while playing
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            activity?.requestedOrientation = previousOrientation
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
            .background(Color.Black)
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
                                // Double tap left: Rewind 10s
                                currentPosition = (currentPosition - 10f).coerceAtLeast(0f)
                                showLeftDoubleTapBadge = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                // Double tap right: Fast Forward 10s
                                currentPosition = (currentPosition + 10f).coerceAtMost(totalDuration)
                                showRightDoubleTapBadge = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onPress = {
                        if (!isLocked) {
                            val pressResult = tryAwaitRelease()
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

        // Overlay Dimming when controls are visible
        if (isControlsVisible && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Top 2x Speed Pill (Non-intrusive layout)
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

        // YouTube-style Double Tap Badges
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

        // Single Floating Lock Button when Locked (No text banner, no notifications)
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
                                text = media.episodes.firstOrNull()?.title ?: "Episode 1",
                                color = custom.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row {
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

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(72.dp)
                            .background(custom.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = custom.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
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

                        TextButton(onClick = { /* Audio & Subtitles */ }) {
                            Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audio & Sub", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Speed Selection Modal Bottom Sheet (No Alert Popups)
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
