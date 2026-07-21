package com.animk.app.ui.component

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.repository.AuthRepository
import com.animk.app.data.repository.ScraperRepository
import com.animk.app.data.repository.SocialRepository
import com.animk.app.data.repository.SupabaseComment
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    media: MediaItem,
    isInMyList: Boolean,
    onDismiss: () -> Unit,
    onPlayEpisode: (MediaItem, Episode) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    onRequireLogin: () -> Unit = {}
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val authRepository = remember { AuthRepository() }
    val socialRepository = remember { SocialRepository() }
    val scraperRepository = remember { ScraperRepository() }

    var likesCount by remember { mutableIntStateOf(128) }
    var dislikesCount by remember { mutableIntStateOf(5) }
    var userInteraction by remember { mutableStateOf<String?>(null) }

    var commentsList by remember { mutableStateOf<List<SupabaseComment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var isPostingComment by remember { mutableStateOf(false) }

    // Scraped episode list state
    var scrapedEpisodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(true) }
    var episodeSource by remember { mutableStateOf("Searching...") }

    // Fetch real episodes from scrapers when sheet opens
    LaunchedEffect(media.title) {
        isLoadingEpisodes = true
        try {
            val episodes = scraperRepository.fetchEpisodesForTitle(media.title)
            scrapedEpisodes = episodes
            val providerCount = episodes.flatMap { it.sources }.map { it.providerKey }.filter { it.isNotBlank() }.distinct().size
            episodeSource = if (episodes.isNotEmpty()) {
                "${episodes.size} episode${if (providerCount > 0) " · $providerCount provider" else ""}"
            } else "No episodes found"
        } catch (e: Exception) {
            episodeSource = "Failed to load episodes"
            e.printStackTrace()
        } finally {
            isLoadingEpisodes = false
        }
    }

    // Fetch Supabase social data
    LaunchedEffect(media.id) {
        try {
            val (likes, dislikes) = socialRepository.getInteractionStats(media.id)
            if (likes > 0 || dislikes > 0) {
                likesCount = likes
                dislikesCount = dislikes
            }
            commentsList = socialRepository.getComments(media.id)
        } catch (_: Exception) {}
    }

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
            // Header Backdrop
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = media.backdropUrl ?: media.posterUrl,
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
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
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
                        Text("${media.matchPercentage}% Match", color = custom.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${media.releaseYear}", color = custom.textSecondary, fontSize = 14.sp)
                        Surface(color = custom.cardSurface, shape = RoundedCornerShape(4.dp)) {
                            Text(media.ageRating, color = custom.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(color = custom.primary, shape = RoundedCornerShape(4.dp)) {
                            Text(media.quality, color = custom.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Play Button → plays first episode
                    Button(
                        onClick = {
                            if (scrapedEpisodes.isNotEmpty()) {
                                onPlayEpisode(media, scrapedEpisodes.first())
                            } else {
                                Toast.makeText(context, "Loading episodes, please wait...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = custom.primary, contentColor = custom.onPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // My List & Download buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleMyList(media) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = custom.textPrimary)
                        ) {
                            Icon(if (isInMyList) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null, tint = if (isInMyList) custom.primary else custom.textPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isInMyList) "In My List" else "My List")
                        }
                        OutlinedButton(
                            onClick = { Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = custom.textPrimary)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(media.description, color = custom.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social Bar
                    Surface(
                        color = custom.cardSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Icon(Icons.Filled.Visibility, contentDescription = "Views", tint = custom.textSecondary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("1.2M", color = custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    if (!authRepository.isUserLoggedIn()) { onRequireLogin(); return@clickable }
                                    userInteraction = "LIKE"; likesCount++
                                    scope.launch { socialRepository.setInteraction(media.id, "LIKE") }
                                }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(if (userInteraction == "LIKE") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, contentDescription = "Like", tint = if (userInteraction == "LIKE") custom.primary else custom.textSecondary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$likesCount", color = if (userInteraction == "LIKE") custom.primary else custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    if (!authRepository.isUserLoggedIn()) { onRequireLogin(); return@clickable }
                                    userInteraction = "DISLIKE"; dislikesCount++
                                    scope.launch { socialRepository.setInteraction(media.id, "DISLIKE") }
                                }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(if (userInteraction == "DISLIKE") Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown, contentDescription = "Dislike", tint = if (userInteraction == "DISLIKE") Color(0xFFEF5350) else custom.textSecondary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$dislikesCount", color = if (userInteraction == "DISLIKE") Color(0xFFEF5350) else custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showCommentsSheet = true }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comments", tint = custom.textSecondary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${commentsList.size}", color = custom.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Episodes Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Episodes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isLoadingEpisodes) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = custom.primary, strokeWidth = 2.dp)
                        } else {
                            Text("($episodeSource)", fontSize = 12.sp, color = custom.textMuted)
                        }
                    }
                }
            }

            // Real Scraped Episodes List
            if (isLoadingEpisodes) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = custom.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Searching episodes from providers...", color = custom.textMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else if (scrapedEpisodes.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "No stream source found for this anime", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(65.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(custom.cardSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = custom.textMuted, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No stream source found", fontWeight = FontWeight.SemiBold, color = custom.textPrimary, fontSize = 14.sp)
                            Text("Scraper couldn't find episodes for this title", color = custom.textMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(scrapedEpisodes, key = { it.id }) { ep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayEpisode(media, ep) }
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
                                model = ep.thumbnailUrl ?: media.backdropUrl ?: media.posterUrl,
                                contentDescription = ep.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Ep", tint = custom.primary, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ep.title, fontWeight = FontWeight.SemiBold, color = custom.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (ep.sources.size > 1) {
                                    Surface(color = custom.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                                        Text("${ep.sources.size} sumber", color = custom.primary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(ep.duration, color = custom.textMuted, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ep.description.ifEmpty { "Tap to stream from provider" }, color = custom.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Comments (${commentsList.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = custom.textPrimary)
                    IconButton(onClick = { showCommentsSheet = false }) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = custom.textSecondary) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (commentsList.isEmpty()) {
                    Text("No comments yet. Be the first to comment!", color = custom.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp)) {
                        items(commentsList) { c ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(custom.primary), contentAlignment = Alignment.Center) {
                                    Text("U", fontWeight = FontWeight.Bold, color = custom.onPrimary, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AnimK User", fontWeight = FontWeight.Bold, color = custom.textPrimary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(c.content, color = custom.textSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...", color = custom.textMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = custom.cardSurface, unfocusedContainerColor = custom.cardSurface, focusedBorderColor = custom.primary, unfocusedBorderColor = Color.Transparent, focusedTextColor = custom.textPrimary, unfocusedTextColor = custom.textPrimary),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (!authRepository.isUserLoggedIn()) { onRequireLogin(); return@IconButton }
                            if (newCommentText.isNotBlank()) {
                                isPostingComment = true
                                scope.launch {
                                    val success = socialRepository.addComment(media.id, newCommentText.trim())
                                    isPostingComment = false
                                    if (success) { newCommentText = ""; commentsList = socialRepository.getComments(media.id) }
                                    else { Toast.makeText(context, "Failed to post comment", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        },
                        enabled = !isPostingComment,
                        modifier = Modifier.background(custom.primary, CircleShape)
                    ) {
                        if (isPostingComment) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = custom.onPrimary)
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = custom.onPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
