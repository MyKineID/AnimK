package com.animk.app.data.scraper

import android.util.Base64
import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val domain = config.domain.trimEnd('/')
            // Otakudesu search page is JS-rendered. Use ongoing page for static HTML.
            val ongoingUrl = "$domain/ongoing-anime/"

            val request = Request.Builder()
                .url(ongoingUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // Structure: .venz ul li .detpost > .thumb a[href] > .thumbz img + h2.jdlflm
            val items = doc.select(".venz ul li .detpost")
            for (element in items) {
                val titleEl = element.selectFirst(".jdlflm")
                val title = titleEl?.text()?.trim() ?: continue
                val linkEl = element.selectFirst(".thumb a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val imgEl = element.selectFirst(".thumbz img")
                val posterUrl = imgEl?.attr("abs:src") ?: imgEl?.attr("abs:data-src") ?: ""

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    // Fuzzy match
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

            // Episodes are links with /episode/ in the URL
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

            // 1. Base64 Decoded Stream Elements
            val elements = doc.select("[data-content]")
            for (el in elements) {
                val base64Content = el.attr("data-content")
                val serverName = el.text().ifEmpty { "OtakuStream ${streams.size + 1}" }
                if (base64Content.isNotBlank()) {
                    try {
                        val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                        val decodedUrl = String(decodedBytes, Charsets.UTF_8)
                        if (decodedUrl.startsWith("http") && addedUrls.add(decodedUrl)) {
                            // If it's a desustream wrapper, fetch actual video URL
                            val resolvedUrl = resolveDesustreamUrl(decodedUrl)
                            streams.add(
                                StreamData(
                                    serverName = serverName,
                                    streamUrl = resolvedUrl,
                                    isIframe = resolvedUrl.contains("<iframe") || !resolvedUrl.endsWith(".mp4"),
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
                    // Resolve desustream wrapper to actual video URL
                    val resolvedUrl = resolveDesustreamUrl(src)
                    val serverLabel = when {
                        src.contains("desustream", ignoreCase = true) -> "DesuStream HD"
                        src.contains("otaku", ignoreCase = true) -> "Otaku Server"
                        src.contains("stream", ignoreCase = true) -> "Stream ${streams.size + 1}"
                        else -> "Server ${streams.size + 1}"
                    }
                    streams.add(
                        StreamData(
                            serverName = serverLabel,
                            streamUrl = resolvedUrl,
                            isIframe = false,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            // 3. Fallback: load episode page directly
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

    /**
     * Resolve desustream wrapper URL to actual video URL.
     * Fetches `?mode=json` and returns the video URL directly.
     * Falls back to original URL if resolution fails.
     */
    private fun resolveDesustreamUrl(wrapperUrl: String): String {
        if (!wrapperUrl.contains("desustream.info")) return wrapperUrl
        return try {
            val jsonUrl = "$wrapperUrl?mode=json&_=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(jsonUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", wrapperUrl)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return wrapperUrl
            val jsonObj = json.parseToJsonElement(body) as? JsonObject
            val ok = jsonObj?.get("ok")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val video = jsonObj?.get("video")?.jsonPrimitive?.content ?: ""
            if (ok && video.isNotBlank()) video else wrapperUrl
        } catch (e: Exception) {
            wrapperUrl
        }
    }
}
