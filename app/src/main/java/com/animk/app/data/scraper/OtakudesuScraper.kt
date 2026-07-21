package com.animk.app.data.scraper

import android.util.Base64
import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class OtakudesuScraper : BaseScraper {
    override val sourceName: String = "Otakudesu"
    override val baseUrl: String = "https://otakudesu.blog"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$baseUrl/?s=$query&post_type=anime"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val items = doc.select("ul.chlist li, ul.chibi li")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select("a").text()
                val posterUrl = element.select("img").attr("src")

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
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

            val epElements = doc.select("ul li a[href*='/episode/']")
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

            // 2. Direct Iframes
            val iframes = doc.select("iframe[src], iframe[data-src], #embed_holder iframe")
            for (iframe in iframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    streams.add(
                        StreamData(
                            serverName = "Otaku Mirror ${streams.size + 1}",
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.SD_480p,
                            priority = ServerPriority.MEDIUM
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
                        resolution = StreamResolution.SD_480p,
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
                    resolution = StreamResolution.SD_480p,
                    priority = ServerPriority.LOW
                )
            )
        }
        streams
    }
}
