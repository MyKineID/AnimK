package com.animk.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableFloatStateOf(120f) }
    val totalDuration = 1440f
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectRatioMode by remember { mutableStateOf("Fit") }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, isControlsVisible) {
        if (isPlaying && isControlsVisible && !isLocked) {
            kotlinx.coroutines.delay(4000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isLocked) {
                    isControlsVisible = !isControlsVisible
                } else {
                    isControlsVisible = true
                }
            }
    ) {
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

        if (isControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { isLocked = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = custom.primary,
                            contentColor = custom.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Unlock")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Screen Locked (Tap to Unlock)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
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
                            IconButton(onClick = { showSpeedDialog = true }) {
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
                                Icon(Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White)
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
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = custom.textPrimary) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    showSpeedDialog = false
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = custom.primary)
                }
            },
            containerColor = custom.surface
        )
    }

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
            }
        }
    }
}
