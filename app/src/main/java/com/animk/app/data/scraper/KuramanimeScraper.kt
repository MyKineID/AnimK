package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class KuramanimeScraper : BaseScraper {
    override val sourceName: String = "Kuramanime"
    override val baseUrl: String = "https://kuramanime.vip"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$baseUrl/anime?search=$query&order_by=popular"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val items = doc.select(".product__item")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select(".product__item__text h5, .product__item__text a").text()
                val posterUrl = element.select(".product__item__pic").attr("data-setbg")
                    .ifEmpty { element.select("img").attr("src") }

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.ANIME,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "Kuramanime stream source",
                            genres = listOf("Action", "Anime")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    override suspend fun getEpisodes(mediaUrl: String): List<Episode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            val request = Request.Builder().url(mediaUrl).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val epElements = doc.select("#episodeLists a, .episode__list a, a[href*='/episode/']")
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

    override suspend fun getStreams(episodeUrl: String): List<StreamData> = withContext(Dispatchers.IO) {
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

            // 1. Direct Video Tags
            val videoSources = doc.select("video source[src], video[src]")
            for (v in videoSources) {
                val src = v.attr("abs:src").ifEmpty { v.attr("src") }
                if (src.isNotBlank() && addedUrls.add(src)) {
                    streams.add(
                        StreamData(
                            serverName = "Kurama Direct HD",
                            streamUrl = src,
                            isIframe = false,
                            resolution = StreamResolution.HD_1080p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            // 2. Extract All Iframes (src & data-src)
            val iframes = doc.select("iframe[src], iframe[data-src], #player iframe, .player iframe")
            for (iframe in iframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                    .ifEmpty { iframe.attr("src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    val serverLabel = when {
                        src.contains("kurama", ignoreCase = true) -> "KuramaDrive Fast"
                        src.contains("beda", ignoreCase = true) -> "BedaDrive HD"
                        src.contains("archive", ignoreCase = true) -> "Archive Server"
                        src.contains("dood", ignoreCase = true) -> "Doodstream"
                        src.contains("streamwish", ignoreCase = true) -> "StreamWish"
                        else -> "Kurama Server ${streams.size + 1}"
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

            // 3. Extract Server Select options & buttons
            val serverOptions = doc.select("select#server option, select.server-list option, #change-server option, a.server-link, [data-stream]")
            for (opt in serverOptions) {
                val value = opt.attr("value").ifEmpty { opt.attr("data-stream") }.ifEmpty { opt.attr("abs:href") }
                val name = opt.text().ifEmpty { "Server ${streams.size + 1}" }
                if (value.startsWith("http") && addedUrls.add(value)) {
                    streams.add(
                        StreamData(
                            serverName = "Kurama - $name",
                            streamUrl = value,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.MEDIUM
                        )
                    )
                }
            }

            // 4. Guaranteed Fallback: Direct Web Player (Episode Page)
            if (addedUrls.add(episodeUrl)) {
                streams.add(
                    StreamData(
                        serverName = "Kuramanime Web Player (Full Page)",
                        streamUrl = episodeUrl,
                        isIframe = true,
                        resolution = StreamResolution.HD_720p,
                        priority = ServerPriority.LOW
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback on error
            streams.add(
                StreamData(
                    serverName = "Kuramanime Web Player (Fallback)",
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
