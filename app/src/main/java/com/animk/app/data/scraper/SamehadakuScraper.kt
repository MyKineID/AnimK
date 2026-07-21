package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class SamehadakuScraper : BaseScraper {
    override val sourceName: String = "Samehadaku"
    override val sourceKey: String = "samehadaku"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val selList = config.selectors.list.ifEmpty { "article.animposx, div.animposx" }
            val selTitle = config.selectors.title.ifEmpty { ".title, h2" }
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
                            description = "Samehadaku stream source",
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

            val epSelector = config.selectors.episodeLink.ifEmpty { ".lepisodes ul li a, .epsselect option, a[href*='/episode/']" }
            val epElements = doc.select(epSelector)
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href").ifEmpty { el.attr("value") }
                val epTitle = el.text().ifEmpty { "Episode $count" }
                if (epUrl.startsWith("http") && !episodes.any { it.sourceUrl == epUrl }) {
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

            val iframeSel = config.selectors.streamIframe.ifEmpty { "iframe[src], iframe[data-src], #embed_holder iframe" }
            val iframes = doc.select(iframeSel)
            for (iframe in iframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    val name = when {
                        src.contains("mega", ignoreCase = true) -> "Mega 1080p"
                        src.contains("wibu", ignoreCase = true) -> "Wibufile HD"
                        src.contains("kraken", ignoreCase = true) -> "Krakenfiles HD"
                        src.contains("pixeldrain", ignoreCase = true) -> "Pixeldrain"
                        src.contains("streamwish", ignoreCase = true) -> "StreamWish"
                        else -> "Samehadaku Server ${streams.size + 1}"
                    }
                    streams.add(
                        StreamData(
                            serverName = name,
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.HD_1080p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            val serverOptions = doc.select("#select-server option, select.mirror option, .server-option option, .server-item a")
            for (opt in serverOptions) {
                val value = opt.attr("value").ifEmpty { opt.attr("abs:href") }
                val label = opt.text().ifEmpty { "Server ${streams.size + 1}" }
                if (value.startsWith("http") && addedUrls.add(value)) {
                    streams.add(
                        StreamData(
                            serverName = "Samehadaku - $label",
                            streamUrl = value,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.MEDIUM
                        )
                    )
                }
            }

            if (addedUrls.add(episodeUrl)) {
                streams.add(
                    StreamData(
                        serverName = "Samehadaku Web Player (Full Page)",
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
                    serverName = "Samehadaku Web Player (Fallback)",
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
