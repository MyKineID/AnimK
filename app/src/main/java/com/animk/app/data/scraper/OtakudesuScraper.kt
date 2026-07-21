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

class OtakudesuScraper : BaseScraper {
    override val sourceName: String = "Otakudesu"
    override val sourceKey: String = "otakudesu"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val selList = config.selectors.list.ifEmpty { "ul.chlist li, ul.chibi li" }
            val selTitle = config.selectors.title.ifEmpty { "a" }
            val selLink = config.selectors.link.ifEmpty { "a[href]" }
            val selImage = config.selectors.image.ifEmpty { "img" }

            val items = doc.select(selList)
            for (element in items) {
                val linkEl = element.selectFirst(selLink)
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select(selTitle).text()
                val posterUrl = element.select(selImage).attr("src")

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

    override suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            val request = Request.Builder().url(mediaUrl).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val epSelector = config.selectors.episodeLink.ifEmpty { "ul li a[href*='/episode/']" }
            val epElements = doc.select(epSelector)
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
            val iframeSel = config.selectors.streamIframe.ifEmpty { "iframe[src], iframe[data-src], #embed_holder iframe" }
            val iframes = doc.select(iframeSel)
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
