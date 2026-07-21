package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class DonghuaScraper : BaseScraper {
    override val sourceName: String = "Anichin Donghua"
    override val baseUrl: String = "https://anichin.my.id"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$baseUrl/?s=$query"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val items = doc.select(".listupd article, .animposx")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select(".title, h2, .entry-title").text()
                val posterUrl = element.select("img").attr("abs:src")
                    .ifEmpty { element.select("img").attr("src") }

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.DONGHUA,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "Anichin Donghua source",
                            genres = listOf("3D Donghua", "Action", "Cultivation")
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

            val epElements = doc.select(".eplister ul li a")
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = el.select(".epl-title").text().ifEmpty { el.text() }
                if (epUrl.isNotBlank()) {
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

    override suspend fun getStreams(episodeUrl: String): List<StreamData> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamData>()
        try {
            val request = Request.Builder().url(episodeUrl).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val mirrorIframes = doc.select(".mirror iframe[src], #embed_holder iframe[src]")
            for (iframe in mirrorIframes) {
                val src = iframe.attr("abs:src")
                if (src.isNotBlank()) {
                    streams.add(
                        StreamData(
                            serverName = "Anichin Mirror",
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.HD_720p,
                            priority = ServerPriority.HIGH
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
