package com.animk.app.ui.screen

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.StreamData
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    episodeSourceUrl: String,
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val scraperRepository = remember { ScraperRepository() }
    var availableStreams by remember { mutableStateOf<List<StreamData>>(emptyList()) }
    var activeStream by remember { mutableStateOf<StreamData?>(null) }
    var isFetchingStreams by remember { mutableStateOf(true) }
    var useWebView by remember { mutableStateOf(false) }

    // ExoPlayer state
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playError by remember { mutableStateOf(false) }

    LaunchedEffect(episodeSourceUrl, media.title) {
        scope.launch {
            isFetchingStreams = true
            val streams = scraperRepository.fetchStreamsForEpisode(episodeSourceUrl, media.title)
            availableStreams = streams
            activeStream = streams.firstOrNull()
            isFetchingStreams = false
        }
    }

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
            exoPlayer?.release()
        }
    }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var is2xSpeedActive by remember { mutableStateOf(false) }

    var showLeftDoubleTapBadge by remember { mutableStateOf(false) }
    var showRightDoubleTapBadge by remember { mutableStateOf(false) }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val swipeProgress = (dragOffsetY.coerceIn(0f, 220f) / 220f)
    val contentAlpha = 1f - (swipeProgress * 0.7f)
    val contentScale = 1f - (swipeProgress * 0.1f)

    LaunchedEffect(showLeftDoubleTapBadge) {
        if (showLeftDoubleTapBadge) { delay(800); showLeftDoubleTapBadge = false }
    }
    LaunchedEffect(showRightDoubleTapBadge) {
        if (showRightDoubleTapBadge) { delay(800); showRightDoubleTapBadge = false }
    }
    LaunchedEffect(isControlsVisible, isLocked) {
        if (isControlsVisible && !isLocked) { delay(5000); isControlsVisible = false }
    }

    LaunchedEffect(playbackSpeed, is2xSpeedActive) {
        val speed = if (is2xSpeedActive) 2.0f else playbackSpeed
        exoPlayer?.setPlaybackSpeed(speed)
    }

    LaunchedEffect(activeStream) {
        activeStream?.let { stream ->
            playError = false
            useWebView = false
            exoPlayer?.apply {
                stop()
                val mediaItem = ExoMediaItem.fromUri(stream.streamUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                setPlaybackSpeed(if (is2xSpeedActive) 2.0f else playbackSpeed)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        AsyncImage(
            model = media.backdropUrl ?: media.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            contentScale = ContentScale.Crop
        )

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
                            if (dragOffsetY > 220f && !isLocked) onBack()
                        },
                        onDragEnd = { dragOffsetY = 0f },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                }
                .pointerInput(isLocked) {
                    detectTapGestures(
                        onTap = { if (!isLocked) isControlsVisible = !isControlsVisible },
                        onDoubleTap = { offset ->
                            if (!isLocked) {
                                val screenWidth = size.width
                                if (offset.x < screenWidth / 2) {
                                    showLeftDoubleTapBadge = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) - 10000)
                                } else {
                                    showRightDoubleTapBadge = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) + 10000)
                                }
                            }
                        },
                        onPress = {
                            if (!isLocked) {
                                tryAwaitRelease()
                                if (is2xSpeedActive) is2xSpeedActive = false
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
            if (useWebView) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.apply {
                                javaScriptEnabled = true; domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36"
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.endsWith(".mp4") || url.endsWith(".m3u8")) {
                                        useWebView = false
                                        exoPlayer?.apply {
                                            stop(); setMediaItem(ExoMediaItem.fromUri(url)); prepare(); playWhenReady = true
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }
                            activeStream?.let { loadUrl(it.streamUrl) }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            val player = ExoPlayer.Builder(ctx)
                                .setSeekBackIncrementMs(10000)
                                .setSeekForwardIncrementMs(10000)
                                .build()
                            exoPlayer = player
                            this.player = player
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            keepScreenOn = true

                            player.addListener(object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) {
                                    isPlaying = playing
                                }
                                override fun onPlayerError(error: PlaybackException) {
                                    playError = true
                                }
                            })

                            activeStream?.let { stream ->
                                val mediaItem = ExoMediaItem.fromUri(stream.streamUrl)
                                player.setMediaItem(mediaItem)
                                player.prepare()
                                player.playWhenReady = true
                                player.setPlaybackSpeed(if (is2xSpeedActive) 2.0f else playbackSpeed)
                            }

                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        }
                    },
                    update = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading indicator
            if (isFetchingStreams) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = custom.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Loading video stream...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Error state
            if (playError && !isFetchingStreams) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Video playback error", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { playError = false; useWebView = true }, colors = ButtonDefaults.buttonColors(containerColor = custom.primary)) {
                                Text("Open in WebView")
                            }
                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeStream?.streamUrl))) },
                                colors = ButtonDefaults.buttonColors(containerColor = custom.primary.copy(alpha = 0.7f))) {
                                Text("Open Browser")
                            }
                        }
                    }
                }
            }

            if (availableStreams.isEmpty() && !isFetchingStreams && !playError) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No active stream servers found", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2x Speed badge
            if (is2xSpeedActive) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
                    Surface(color = Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FastForward, contentDescription = null, tint = custom.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("2x Speeding", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Double tap seek badges
            AnimatedVisibility(visible = showLeftDoubleTapBadge, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)) {
                Surface(color = Color.Black.copy(alpha = 0.75f), shape = CircleShape, modifier = Modifier.size(70.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = custom.primary)
                        Text("-10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            AnimatedVisibility(visible = showRightDoubleTapBadge, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)) {
                Surface(color = Color.Black.copy(alpha = 0.75f), shape = CircleShape, modifier = Modifier.size(70.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = custom.primary)
                        Text("+10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Lock overlay
            if (isLocked && isControlsVisible) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = { isLocked = false }, modifier = Modifier.background(custom.primary.copy(alpha = 0.85f), CircleShape)) {
                        Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = custom.onPrimary)
                    }
                }
            }

            // Controls overlay
            if (isControlsVisible && !isLocked) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))))
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))

                    Row(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            Spacer(Modifier.width(8.dp))
                            Text(text = media.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { isLocked = true }) { Icon(Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White.copy(alpha = 0.85f)) }
                    }

                    Box(modifier = Modifier.align(Alignment.Center).fillMaxSize().clickable {
                        exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                    })

                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row {
                                TextButton(onClick = { showServerSheet = true }) {
                                    Icon(Icons.Filled.Dns, contentDescription = null, tint = custom.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(activeStream?.serverName ?: "Select Server", color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Surface(color = custom.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text("${availableStreams.size}", color = custom.primary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { showSpeedSheet = true }) {
                                    Icon(Icons.Filled.Speed, contentDescription = null, tint = custom.primary.copy(alpha = 0.8f))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = { useWebView = !useWebView }) {
                                Icon(Icons.Filled.Language, contentDescription = null, tint = custom.primary.copy(alpha = 0.8f))
                                Spacer(Modifier.width(4.dp))
                                Text(if (useWebView) "ExoPlayer" else "WebView", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Speed Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }, containerColor = custom.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Playback Speed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                Spacer(Modifier.height(12.dp))
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { playbackSpeed = speed; showSpeedSheet = false }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                            color = if (playbackSpeed == speed) custom.primary else custom.textPrimary,
                            fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                        if (playbackSpeed == speed) Icon(Icons.Filled.Check, contentDescription = null, tint = custom.primary)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Server Sheet
    if (showServerSheet) {
        ModalBottomSheet(onDismissRequest = { showServerSheet = false }, containerColor = custom.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Dns, contentDescription = null, tint = custom.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Select Stream Server (${availableStreams.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                }
                Text("If playback fails, try another server or switch to WebView.", fontSize = 12.sp, color = custom.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                availableStreams.forEach { stream ->
                    Surface(color = if (activeStream == stream) custom.primary.copy(alpha = 0.15f) else custom.cardSurface,
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { activeStream = stream; showServerSheet = false }) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = stream.serverName, color = if (activeStream == stream) custom.primary else custom.textPrimary, fontSize = 14.sp)
                                Text(text = stream.streamUrl, color = custom.textMuted, fontSize = 11.sp, maxLines = 1)
                            }
                            if (activeStream == stream) Icon(Icons.Filled.Check, contentDescription = null, tint = custom.primary)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
