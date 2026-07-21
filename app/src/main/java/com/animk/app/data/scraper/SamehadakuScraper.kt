package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class SamehadakuScraper : BaseScraper {
    override val sourceName: String = "Samehadaku"
    override val baseUrl: String = "https://v2.samehadaku.how"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$baseUrl/?s=$query"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            val items = doc.select(".animepost, article.animposx")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select(".title, .entry-title, h2").text()
                val posterUrl = element.select("img").attr("abs:src")
                    .ifEmpty { element.select("img").attr("src") }

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.ANIME,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "Samehadaku streaming source"
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

            val epElements = doc.select(".lister ephash, .lister ul li a, .epsselect option")
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href").ifEmpty { el.attr("value") }
                val epTitle = el.text().ifEmpty { "Episode $count" }
                if (epUrl.isNotBlank() && epUrl.startsWith("http")) {
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

            val iframes = doc.select("iframe[src]")
            for (iframe in iframes) {
                val src = iframe.attr("abs:src")
                if (src.isNotBlank()) {
                    val name = when {
                        src.contains("mega", ignoreCase = true) -> "Mega 1080p"
                        src.contains("wibu", ignoreCase = true) -> "Wibufile HD"
                        else -> "Samehadaku Server"
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }
}
