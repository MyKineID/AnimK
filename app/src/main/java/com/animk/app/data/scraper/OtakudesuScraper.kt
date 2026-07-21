package com.animk.app.data.scraper

import android.util.Base64
import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * Otakudesu scraper.
 *
 * NOTE: Otakudesu search page is JavaScript-rendered and cannot be parsed by Jsoup.
 * Instead, search uses the ongoing page (`/ongoing-anime/`) which is static HTML.
 */
class OtakudesuScraper : BaseScraper {
    override val sourceName: String = "Otakudesu"
    override val sourceKey: String = "otakudesu"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            // Otakudesu search page is JS-rendered. Use ongoing page for static HTML.
            val domain = config.domain.trimEnd('/')
            val ongoingUrl = "$domain/ongoing-anime/"

            val request = Request.Builder()
                .url(ongoingUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // Otakudesu ongoing page structure:
            // <div class="venz"><ul><li><div class='detpost'>
            //   <div class="thumb"><a href="..."><div class="thumbz">
            //     <img src="..." /><h2 class="jdlflm">Title</h2>
            val items = doc.select(".venz ul li .detpost")
            for (element in items) {
                val titleEl = element.selectFirst(".jdlflm")
                val title = titleEl?.text()?.trim() ?: continue
                val linkEl = element.selectFirst(".thumb a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val imgEl = element.selectFirst(".thumbz img")
                val posterUrl = imgEl?.attr("abs:src") ?: imgEl?.attr("abs:data-src") ?: ""
                val epEl = element.selectFirst(".epz")
                val episodeCount = epEl?.text()?.trim() ?: ""

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    // Case-insensitive fuzzy match
                    if (title.contains(query, ignoreCase = true) || query.contains(title, ignoreCase = true)) {
                        list.add(
                            MediaItem(
                                id = mediaUrl,
                                title = title,
                                type = MediaType.ANIME,
                                posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                                backdropUrl = posterUrl,
                                description = "Otakudesu stream source",
                                genres = listOf("Action", "Anime")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    override suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            val request = Request.Builder()
                .url(mediaUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // Otakudesu detail page episodes are links with /episode/ in the URL
            // Inside <div class="episodelist">
            val epElements = doc.select("a[href*='/episode/']")
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = el.text().ifEmpty { "Episode $count" }
                if (epUrl.isNotBlank() && !episodes.any { it.sourceUrl == epUrl }) {
                    episodes.add(
                        Episode(
                            id = epUrl,
                            sourceUrl = epUrl,
                            episodeNumber = count,
                            title = epTitle
                        )
                    )
                    count += 1f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (episodes.isEmpty()) {
            episodes.add(Episode(id = mediaUrl, sourceUrl = mediaUrl, episodeNumber = 1f, title = "Episode 1"))
        }
        episodes
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamData>()
        val addedUrls = mutableSetOf<String>()

        try {
            val request = Request.Builder()
                .url(episodeUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // 1. Base64 Decoded Stream Elements (otakudesu-specific)
            val elements = doc.select("[data-content]")
            for (el in elements) {
                val base64Content = el.attr("data-content")
                val serverName = el.text().ifEmpty { "OtakuStream ${streams.size + 1}" }
                if (base64Content.isNotBlank()) {
                    try {
                        val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                        val decodedUrl = String(decodedBytes, Charsets.UTF_8)
                        if (decodedUrl.startsWith("http") && addedUrls.add(decodedUrl)) {
                            streams.add(
                                StreamData(
                                    serverName = serverName,
                                    streamUrl = decodedUrl,
                                    isIframe = decodedUrl.contains("<iframe") || !decodedUrl.endsWith(".mp4"),
                                    resolution = StreamResolution.HD_720p,
                                    priority = ServerPriority.HIGH
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 2. Direct Iframes (desustream.info etc.)
            val iframeSel = config.selectors.streamIframe.ifEmpty { "iframe[src]" }
            val iframes = doc.select(iframeSel)
            for (iframe in iframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    val serverLabel = when {
                        src.contains("desustream", ignoreCase = true) -> "DesuStream HD"
                        src.contains("otaku", ignoreCase = true) -> "Otaku Server"
                        src.contains("stream", ignoreCase = true) -> "Stream ${streams.size + 1}"
                        else -> "Server ${streams.size + 1}"
                    }
                    streams.add(
                        StreamData(
                            serverName = serverLabel,
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            // 3. Fallback Full Page Player
            if (addedUrls.add(episodeUrl)) {
                streams.add(
                    StreamData(
                        serverName = "Otakudesu Web Player (Full Page)",
                        streamUrl = episodeUrl,
                        isIframe = true,
                        resolution = StreamResolution.HD_720p,
                        priority = ServerPriority.LOW
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            streams.add(
                StreamData(
                    serverName = "Otakudesu Web Player (Fallback)",
                    streamUrl = episodeUrl,
                    isIframe = true,
                    resolution = StreamResolution.HD_720p,
                    priority = ServerPriority.LOW
                )
            )
        }
        streams
    }
}
