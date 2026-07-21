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

            val epElements = doc.select("#episodeLists a, .episode__list a")
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = el.text().ifEmpty { "Episode $count" }
                if (epUrl.isNotBlank()) {
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
        try {
            val request = Request.Builder().url(episodeUrl).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val iframeEl = doc.selectFirst("iframe[src]")
            val videoEl = doc.selectFirst("video source[src]")

            if (videoEl != null) {
                val src = videoEl.attr("abs:src")
                streams.add(
                    StreamData(
                        serverName = "KuramaDrive Direct",
                        streamUrl = src,
                        isIframe = false,
                        resolution = StreamResolution.HD_1080p,
                        priority = ServerPriority.HIGH
                    )
                )
            } else if (iframeEl != null) {
                val src = iframeEl.attr("abs:src")
                streams.add(
                    StreamData(
                        serverName = "KuramaDrive Stream",
                        streamUrl = src,
                        isIframe = true,
                        resolution = StreamResolution.HD_720p,
                        priority = ServerPriority.HIGH
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }
}
