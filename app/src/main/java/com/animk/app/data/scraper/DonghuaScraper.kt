package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class DonghuaScraper : BaseScraper {
    override val sourceName: String = "Anichin"
    override val sourceKey: String = "donghua"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val selList = config.selectors.list.ifEmpty { "article.animposx, div.bs" }
            val selTitle = config.selectors.title.ifEmpty { ".title, h2, .tt" }
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
                            type = MediaType.DONGHUA,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "Anichin Donghua 3D stream source",
                            genres = listOf("3D", "Donghua", "Action")
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

            val epSelector = config.selectors.episodeLink.ifEmpty { ".eplister ul li a, a[href*='/episode/']" }
            val epElements = doc.select(epSelector)
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = el.select(".epl-num, .epl-title").text().ifEmpty { el.text() }
                if (epUrl.isNotBlank() && !episodes.any { it.sourceUrl == epUrl }) {
                    episodes.add(
                        Episode(
                            id = epUrl,
                            sourceUrl = epUrl,
                            episodeNumber = count,
                            title = if (epTitle.isNotBlank()) epTitle else "Episode $count"
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

            val mirrorIframes = doc.select(
                config.selectors.streamIframe.ifEmpty { ".mirror iframe[src], #embed_holder iframe[src], iframe[data-src]" }
            )
            for (iframe in mirrorIframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    streams.add(
                        StreamData(
                            serverName = "Anichin Mirror ${streams.size + 1}",
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            val mirrorOptions = doc.select("select.mirror option, .mirror option")
            for (opt in mirrorOptions) {
                val value = opt.attr("value")
                val name = opt.text().ifEmpty { "Anichin Server ${streams.size + 1}" }
                if (value.startsWith("http") && addedUrls.add(value)) {
                    streams.add(
                        StreamData(
                            serverName = name,
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
                        serverName = "Anichin Web Player (Full Page)",
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
                    serverName = "Anichin Web Player (Fallback)",
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
