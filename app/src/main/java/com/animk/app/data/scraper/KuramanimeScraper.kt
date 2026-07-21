package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class KuramanimeScraper : BaseScraper {
    override val sourceName: String = "Kuramanime"
    override val sourceKey: String = "kuramanime"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val searchPath = config.searchPath.ifEmpty { "/anime?search=" }
            val url = config.domain.trimEnd('/') + searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, config.domain)

            val selList = config.selectors.list.ifEmpty { ".product__item" }
            val selTitle = config.selectors.title.ifEmpty { ".product__item__text h5, .product__item__text a" }
            val selLink = config.selectors.link.ifEmpty { "a[href]" }
            val selImage = config.selectors.image.ifEmpty { ".product__item__pic" }

            val items = doc.select(selList)
            for (element in items) {
                val linkEl = element.selectFirst(selLink)
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = cleanProviderTitle(element.selectFirst(selTitle)?.text().orEmpty())
                val posterUrl = element.selectFirst(selImage)?.attr("data-setbg").orEmpty()
                    .ifEmpty { element.selectFirst("img")?.attr("abs:src").orEmpty() }

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

    override suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            val request = Request.Builder().url(mediaUrl).build()
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, mediaUrl)

            val epSelector = config.selectors.episodeLink.ifEmpty { "#episodeLists a, .episode__list a, a[href*='/episode/']" }
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

            // 1. Direct Video Tags
            val videoSel = config.selectors.streamVideo.ifEmpty { "video source[src], video[src]" }
            val videoSources = doc.select(videoSel)
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

            // 2. Extract All Iframes
            val iframeSel = config.selectors.streamIframe.ifEmpty { "iframe[src], iframe[data-src], #player iframe, .player iframe" }
            val iframes = doc.select(iframeSel)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }
}
