package com.animk.app.ui.screen

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.animk.app.data.model.EpisodeSource
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.StreamData
import com.animk.app.data.model.StreamResolution
import com.animk.app.data.playback.WatchHistoryStore
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

private const val BLOGGER_PLAYER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"

private fun StreamResolution.displayName(): String = when (this) {
    StreamResolution.HD_1080p -> "1080p"
    StreamResolution.HD_720p -> "720p"
    StreamResolution.SD_480p -> "480p"
    StreamResolution.SD_360p -> "360p"
    StreamResolution.UNKNOWN -> "Auto"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetflixMediaPlayerScreen(
    media: MediaItem,
    episodeSourceUrl: String,
    episodeSources: List<EpisodeSource> = emptyList(),
    onBack: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    BackHandler(onBack = onBack)

    val scraperRepository = remember { ScraperRepository() }
    var availableStreams by remember { mutableStateOf<List<StreamData>>(emptyList()) }
    var activeStream by remember { mutableStateOf<StreamData?>(null) }
    var isFetchingStreams by remember { mutableStateOf(true) }
    var failedStreamUrls by remember(episodeSourceUrl) { mutableStateOf(emptySet<String>()) }
    var playErrorMessage by remember { mutableStateOf("") }

    // ExoPlayer state
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var playbackDurationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var playError by remember { mutableStateOf(false) }

    fun tryNextStream(failedUrl: String? = activeStream?.streamUrl) {
        failedUrl?.let { failedStreamUrls = failedStreamUrls + it }
        val next = availableStreams.firstOrNull { it.streamUrl !in failedStreamUrls }
        activeStream = next
        playError = next == null
        playErrorMessage = if (next == null) "Semua server direct gagal diputar" else ""
    }

    var resumePositionMs by remember(episodeSourceUrl) {
        mutableLongStateOf(WatchHistoryStore.positionFor(episodeSourceUrl))
    }
    var hasRestoredPosition by remember(episodeSourceUrl) { mutableStateOf(false) }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        WatchHistoryStore.saveProgress(media, episodeSourceUrl, media.title, positionMs, durationMs)
    }

    fun syncPlaybackUi(player: ExoPlayer) {
        playbackPositionMs = player.currentPosition.coerceAtLeast(0L)
        playbackDurationMs = player.duration.coerceAtLeast(0L)
        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(playbackPositionMs)
        isPlaying = player.isPlaying
    }

    fun formatTime(milliseconds: Long): String {
        val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000).toInt()
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    LaunchedEffect(episodeSourceUrl, episodeSources, media.title) {
        isFetchingStreams = true
        val sources = episodeSources.ifEmpty {
            listOf(EpisodeSource(providerKey = "", providerName = "", sourceUrl = episodeSourceUrl))
        }
        val streams = scraperRepository.fetchStreamsForEpisodes(sources, media.title)
        failedStreamUrls = emptySet()
        availableStreams = streams
        activeStream = streams.firstOrNull()
        playErrorMessage = if (streams.isEmpty()) "Tidak ada stream direct yang berhasil diambil" else ""
        isFetchingStreams = false
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
            exoPlayer?.let { saveProgress(it.currentPosition, it.duration) }
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
            playErrorMessage = ""
            resumePositionMs = WatchHistoryStore.positionFor(episodeSourceUrl)
            hasRestoredPosition = false
            // Provider pages are never rendered in the app. Only direct media URLs
            // are accepted so playback and controls always remain in AnimK's player.
            if (stream.isIframe) {
                playError = true
            } else {
                exoPlayer?.apply {
                    stop()
                    setMediaItem(ExoMediaItem.fromUri(stream.streamUrl))
                    if (resumePositionMs > 1_000L) {
                        seekTo(resumePositionMs)
                        hasRestoredPosition = true
                    }
                    prepare()
                    playWhenReady = true
                    setPlaybackSpeed(if (is2xSpeedActive) 2.0f else playbackSpeed)
                }
            }
        }
    }

    LaunchedEffect(exoPlayer, activeStream) {
        var lastSavedAt = 0L
        while (true) {
            delay(400)
            exoPlayer?.let { player ->
                syncPlaybackUi(player)
                val now = System.currentTimeMillis()
                if (player.isPlaying && now - lastSavedAt >= 10_000L) {
                    saveProgress(player.currentPosition, player.duration)
                    lastSavedAt = now
                }
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
        ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            // A fresh HTTP source is created for each selected stream so
                            // its signed URL gets the correct Referer without rebuilding UI.
                            val dataSourceFactory = DataSource.Factory {
                                DefaultHttpDataSource.Factory()
                                    .setUserAgent(BLOGGER_PLAYER_USER_AGENT)
                                    .setDefaultRequestProperties(activeStream?.additionalHeaders ?: emptyMap())
                                    .setAllowCrossProtocolRedirects(true)
                                    .createDataSource()
                            }
                            val player = ExoPlayer.Builder(ctx)
                                .setMediaSourceFactory(
                                    DefaultMediaSourceFactory(ctx).setDataSourceFactory(dataSourceFactory)
                                )
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
                                override fun onEvents(player: Player, events: Player.Events) {
                                    syncPlaybackUi(player as ExoPlayer)
                                }
                                override fun onPlayerError(error: PlaybackException) {
                                    playErrorMessage = error.errorCodeName
                                    tryNextStream()
                                }
                            })

                            activeStream?.let { stream ->
                                val mediaItem = ExoMediaItem.fromUri(stream.streamUrl)
                                player.setMediaItem(mediaItem)
                                if (resumePositionMs > 1_000L && !hasRestoredPosition) {
                                    player.seekTo(resumePositionMs)
                                    hasRestoredPosition = true
                                }
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

            // Native gesture layer: no Material ripple and never changes play state on a single tap.
            if (!isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    if (dragOffsetY > 220f) onBack()
                                },
                                onDragEnd = { dragOffsetY = 0f },
                                onDragCancel = { dragOffsetY = 0f }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isControlsVisible = !isControlsVisible },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) {
                                        showLeftDoubleTapBadge = true
                                        exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0L) - 10_000L)
                                    } else {
                                        showRightDoubleTapBadge = true
                                        exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0L) + 10_000L)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onPress = {
                                    tryAwaitRelease()
                                    if (is2xSpeedActive) is2xSpeedActive = false
                                },
                                onLongPress = {
                                    is2xSpeedActive = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
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
                        if (playErrorMessage.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(playErrorMessage, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (availableStreams.any { it.streamUrl !in failedStreamUrls }) {
                                    tryNextStream()
                                } else {
                                    playError = false
                                    exoPlayer?.apply { stop(); prepare(); playWhenReady = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = custom.primary)
                        ) { Text(if (availableStreams.any { it.streamUrl !in failedStreamUrls }) "Coba server lain" else "Coba lagi") }
                    }
                }
            }

            if (availableStreams.isEmpty() && !isFetchingStreams && !playError) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No active stream servers found", color = Color.White, fontWeight = FontWeight.Bold)
                        if (playErrorMessage.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(playErrorMessage, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                        }
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

                    Row(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { isLocked = true; isControlsVisible = true }) {
                            Icon(Icons.Filled.LockOpen, contentDescription = "Kunci kontrol", tint = Color.White.copy(alpha = 0.9f))
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.FullscreenExit, contentDescription = "Keluar layar penuh", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0L) - 10_000L) },
                            modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Mundur 10 detik", tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                        IconButton(
                            onClick = { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } },
                            modifier = Modifier.size(54.dp).background(Color.Black.copy(alpha = 0.58f), CircleShape)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(
                            onClick = { exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0L) + 10_000L) },
                            modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Maju 10 detik", tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                    }

                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        val sliderMax = playbackDurationMs.coerceAtLeast(1L).toFloat()
                        val sliderValue = if (isScrubbing) scrubPositionMs else playbackPositionMs.coerceAtMost(playbackDurationMs).toFloat()
                        Slider(
                            value = sliderValue.coerceIn(0f, sliderMax),
                            onValueChange = { value ->
                                isScrubbing = true
                                scrubPositionMs = value
                            },
                            onValueChangeFinished = {
                                exoPlayer?.seekTo(scrubPositionMs.toLong())
                                isScrubbing = false
                            },
                            valueRange = 0f..sliderMax,
                            enabled = playbackDurationMs > 0L,
                            colors = SliderDefaults.colors(
                                thumbColor = custom.primary,
                                activeTrackColor = custom.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(if (isScrubbing) scrubPositionMs.toLong() else playbackPositionMs), color = Color.White, fontSize = 12.sp)
                            Text(formatTime(playbackDurationMs), color = Color.White, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 36.dp).clickable { showServerSheet = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Dns, contentDescription = "Pilih server", tint = custom.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = activeStream?.let { "${it.providerName.ifBlank { "Server" }} · ${it.resolution.displayName()}" } ?: "Pilih server",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(color = custom.primary.copy(alpha = 0.22f), shape = CircleShape) {
                                        Text("${availableStreams.size}", color = custom.primary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color.Black.copy(alpha = 0.42f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp).clickable { showSpeedSheet = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
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

    // Compact native server picker; it never opens a provider webpage.
    if (showServerSheet) {
        AlertDialog(
            onDismissRequest = { showServerSheet = false },
            containerColor = custom.surface,
            titleContentColor = custom.textPrimary,
            textContentColor = custom.textSecondary,
            title = {
                Column {
                    Text("Server & kualitas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${availableStreams.size} stream direct tersedia", fontSize = 12.sp, color = custom.textMuted, fontWeight = FontWeight.Normal)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 310.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableStreams, key = { "${it.providerName}:${it.serverName}:${it.streamUrl}" }) { stream ->
                        val selected = activeStream == stream
                        Surface(
                            color = if (selected) custom.primary.copy(alpha = 0.14f) else custom.cardSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                activeStream = stream
                                showServerSheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = if (selected) custom.primary else custom.background, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        stream.resolution.displayName(),
                                        color = if (selected) custom.onPrimary else custom.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stream.providerName.ifBlank { "Direct stream" },
                                        color = if (selected) custom.primary else custom.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(stream.serverName, color = custom.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "Aktif", tint = custom.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showServerSheet = false }) { Text("Tutup", color = custom.primary) }
            }
        )
    }
}
