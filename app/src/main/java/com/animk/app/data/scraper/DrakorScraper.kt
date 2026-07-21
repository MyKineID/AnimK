package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class DrakorScraper : BaseScraper {
    override val sourceName: String = "JuraganFilm"
    override val sourceKey: String = "drakor"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, config.domain)

            val selList = config.selectors.list.ifEmpty { ".item, article" }
            val selTitle = config.selectors.title.ifEmpty { "h2, .entry-title, .title" }
            val selLink = config.selectors.link.ifEmpty { "a[href]" }
            val selImage = config.selectors.image.ifEmpty { "img" }

            val items = doc.select(selList)
            for (element in items) {
                val linkEl = element.selectFirst(selLink)
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = cleanProviderTitle(element.selectFirst(selTitle)?.text().orEmpty())
                val posterUrl = element.selectFirst(selImage)?.attr("abs:src").orEmpty()
                    .ifBlank { element.selectFirst(selImage)?.attr("abs:data-src").orEmpty() }

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.DRAKOR,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "JuraganFilm K-Drama stream source",
                            genres = listOf("Drama", "Romance", "K-Drama")
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
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, mediaUrl)

            val epSelector = config.selectors.episodeLink.ifEmpty { ".episodelist ul li a, a[href*='/episode/']" }
            val epElements = doc.select(epSelector)
            var fallbackNumber = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = cleanProviderTitle(el.text())
                val number = episodeNumberFromText(epTitle) ?: fallbackNumber
                if (epUrl.isNotBlank() && !episodes.any { it.sourceUrl == epUrl }) {
                    episodes.add(
                        Episode(
                            id = epUrl,
                            sourceUrl = epUrl,
                            episodeNumber = number,
                            title = epTitle.ifBlank { "Episode ${number.toInt()}" }
                        )
                    )
                    fallbackNumber += 1f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            val playerAreaIframes = doc.select(
                config.selectors.streamIframe.ifEmpty { ".player-area iframe[src], #player-option iframe[src], iframe[data-src]" }
            )
            for (iframe in playerAreaIframes) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    streams.add(
                        StreamData(
                            serverName = "JuraganFilm Player ${streams.size + 1}",
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.HIGH
                        )
                    )
                }
            }

            val serverOptions = doc.select("select#player-select option, .server-list option")
            for (opt in serverOptions) {
                val value = opt.attr("value")
                val name = opt.text().ifEmpty { "Juragan Server ${streams.size + 1}" }
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }
}
