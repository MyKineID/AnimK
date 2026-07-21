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

            val items = doc.select(".chivsrc, ul.chseries li")
            for (element in items) {
                val linkEl = element.selectFirst("a[href]")
                val mediaUrl = linkEl?.attr("abs:href") ?: ""
                val title = element.select("h2, a").text()
                val posterUrl = element.select("img").attr("abs:src")

                if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                    list.add(
                        MediaItem(
                            id = mediaUrl,
                            title = title,
                            type = MediaType.ANIME,
                            posterUrl = posterUrl.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = posterUrl,
                            description = "Otakudesu anime source"
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

            val epElements = doc.select(".episodelist ul li a")
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val epTitle = el.text()
                if (epUrl.isNotBlank() && epUrl.contains("/episode/")) {
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

            val elements = doc.select("[data-content]")
            for (el in elements) {
                val base64Content = el.attr("data-content")
                if (base64Content.isNotBlank()) {
                    try {
                        val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                        val decodedUrl = String(decodedBytes, Charsets.UTF_8)
                        if (decodedUrl.startsWith("http")) {
                            streams.add(
                                StreamData(
                                    serverName = "OtakuStream Direct",
                                    streamUrl = decodedUrl,
                                    isIframe = decodedUrl.contains("<iframe"),
                                    resolution = StreamResolution.HD_720p,
                                    priority = ServerPriority.MEDIUM
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (streams.isEmpty()) {
                val iframe = doc.selectFirst("iframe[src]")
                if (iframe != null) {
                    val src = iframe.attr("abs:src")
                    streams.add(
                        StreamData(
                            serverName = "Otakudesu Mirror",
                            streamUrl = src,
                            isIframe = true,
                            resolution = StreamResolution.SD_480p,
                            priority = ServerPriority.LOW
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
