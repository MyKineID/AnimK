package com.animk.app.ui.screen

import android.content.pm.ActivityInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    LaunchedEffect(episodeSourceUrl, media.title) {
        scope.launch {
            isFetchingStreams = true
            val streams = scraperRepository.fetchStreamsForEpisode(episodeSourceUrl, media.title)
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

    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var is2xSpeedActive by remember { mutableStateOf(false) }

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

    LaunchedEffect(isControlsVisible, isLocked) {
        if (isControlsVisible && !isLocked) {
            delay(5000)
            isControlsVisible = false
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    LaunchedEffect(playbackSpeed) {
        webViewRef?.evaluateJavascript("if (document.querySelector('video')) { document.querySelector('video').playbackRate = $playbackSpeed; }", null)
    }

    LaunchedEffect(is2xSpeedActive) {
        val speed = if (is2xSpeedActive) 2.0f else playbackSpeed
        webViewRef?.evaluateJavascript("if (document.querySelector('video')) { document.querySelector('video').playbackRate = $speed; }", null)
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
                                    showLeftDoubleTapBadge = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    webViewRef?.evaluateJavascript("if (document.querySelector('video')) { document.querySelector('video').currentTime -= 10; }", null)
                                } else {
                                    showRightDoubleTapBadge = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    webViewRef?.evaluateJavascript("if (document.querySelector('video')) { document.querySelector('video').currentTime += 10; }", null)
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
            // WebView Video Player
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        webViewRef = this
                    }
                },
                update = { webView ->
                    activeStream?.let { stream ->
                        if (stream.streamUrl != lastLoadedUrl) {
                            lastLoadedUrl = stream.streamUrl
                            if (stream.isIframe) {
                                if (stream.streamUrl.startsWith("http")) {
                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                                iframe { width: 100%; height: 100%; border: none; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe src="${stream.streamUrl}" allowfullscreen="true" allow="autoplay; fullscreen; picture-in-picture"></iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    webView.loadDataWithBaseURL(stream.streamUrl, html, "text/html", "UTF-8", null)
                                } else {
                                    webView.loadUrl(stream.streamUrl)
                                }
                            } else {
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; }</style>
                                    </head>
                                    <body>
                                        <video width="100%" height="100%" autoplay controls style="object-fit:contain">
                                            <source src="${stream.streamUrl}" type="video/mp4">
                                        </video>
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.loadData(html, "text/html", "UTF-8")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading indicator
            if (isFetchingStreams) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = custom.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gathering servers from all providers...", color = Color.White, fontSize = 13.sp)
                    }
                }
            } else if (availableStreams.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active stream servers found", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Tap back and try selecting another episode", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }

            // Subtle gradient overlay when controls visible
            if (isControlsVisible && !isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
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
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
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
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FastForward, contentDescription = null, tint = custom.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2x Speeding", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Double Tap Left Feedback (-10s)
            AnimatedVisibility(
                visible = showLeftDoubleTapBadge,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = custom.primary)
                        Text("-10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Double Tap Right Feedback (+10s)
            AnimatedVisibility(
                visible = showRightDoubleTapBadge,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = custom.primary)
                        Text("+10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Locked Screen Floating Unlock Button
            if (isLocked && isControlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier.background(custom.primary.copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = custom.onPrimary)
                    }
                }
            }

            // Overlay Controls
            if (isControlsVisible && !isLocked) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        IconButton(onClick = { isLocked = true }) {
                            Icon(Icons.Filled.LockOpen, contentDescription = "Lock Screen", tint = Color.White.copy(alpha = 0.85f))
                        }
                    }

                    // Bottom Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                TextButton(onClick = { showServerSheet = true }) {
                                    Icon(Icons.Filled.Dns, contentDescription = "Server", tint = custom.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(activeStream?.serverName ?: "Select Server", color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(color = custom.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text("${availableStreams.size} Servers", color = custom.primary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(onClick = { showSpeedSheet = true }) {
                                    Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = custom.primary.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                }
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
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Multi-Server Selection Modal Sheet
    if (showServerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showServerSheet = false },
            containerColor = custom.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Dns, contentDescription = null, tint = custom.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Stream Server (${availableStreams.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                }
                Text("If a server shows a black screen, select another server from the list below.", fontSize = 12.sp, color = custom.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                
                availableStreams.forEach { stream ->
                    Surface(
                        color = if (activeStream == stream) custom.primary.copy(alpha = 0.15f) else custom.cardSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                activeStream = stream
                                showServerSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stream.serverName,
                                    color = if (activeStream == stream) custom.primary else custom.textPrimary,
                                    fontWeight = if (activeStream == stream) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (stream.isIframe) "Embed Stream" else "Direct MP4 Stream",
                                    color = custom.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (activeStream == stream) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = custom.primary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
