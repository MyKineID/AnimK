package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class DrakorScraper : BaseScraper {
    override val sourceName: String = "JuraganFilm Drakor"
    override val baseUrl: String = "https://tv48.juragan.film"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$baseUrl/?s=$query"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val items = doc.select("article, .post-item")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select(".entry-title, h2, h3").text()
                val posterUrl = element.select("img").attr("abs:src")
                    .ifEmpty { element.select("img").attr("src") }

                if (title.isNotBlank() && mediaUrl.isNotBlank() && (mediaUrl.contains("/k-drama/") || title.contains("Drakor", ignoreCase = true))) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.DRAKOR,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "JuraganFilm K-Drama source",
                            genres = listOf("Drama", "Romance", "Korean")
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

            val epElements = doc.select(".episode-list a, .player-episodes a")
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

            val playerAreaIframes = doc.select(".player-area iframe[src], #player-option iframe[src]")
            for (iframe in playerAreaIframes) {
                val src = iframe.attr("abs:src")
                if (src.isNotBlank()) {
                    streams.add(
                        StreamData(
                            serverName = "JuraganFilm Player",
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
