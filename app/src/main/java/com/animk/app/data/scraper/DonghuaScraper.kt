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

class DonghuaScraper : BaseScraper {
    override val sourceName: String = "Anichin"
    override val sourceKey: String = "anichin"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).build()
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, config.domain)

            val selList = config.selectors.list.ifEmpty { "article.animposx, div.bs" }
            val selTitle = config.selectors.title.ifEmpty { ".title, h2, .tt" }
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
            val html = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, mediaUrl)

            val epSelector = config.selectors.episodeLink.ifEmpty { ".eplister ul li a, a[href*='/episode/']" }
            val epElements = doc.select(epSelector)
            var count = 1f
            for (el in epElements) {
                val epUrl = el.attr("abs:href")
                val detectedNumber = el.attr("data-number").toFloatOrNull()
                    ?: episodeNumberFromText(el.text())
                val number = detectedNumber ?: count
                val epTitle = cleanProviderTitle(el.select(".epl-num, .epl-title").text().ifEmpty { el.text() })
                if (epUrl.isNotBlank() && !episodes.any { it.sourceUrl == epUrl }) {
                    episodes.add(
                        Episode(
                            id = epUrl,
                            sourceUrl = epUrl,
                            episodeNumber = number,
                            title = when {
                                detectedNumber != null && detectedNumber <= 0f -> "Special"
                                epTitle.contains("episode", ignoreCase = true) -> epTitle
                                else -> "Episode ${if (number % 1f == 0f) number.toInt() else number}"
                            }
                        )
                    )
                    count += 1f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // A detail page is not an episode page. Never invent Episode 1 here;
        // doing so sends Media3 to HTML and creates a misleading empty player.
        episodes
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamData>()
        val addedUrls = mutableSetOf<String>()
        try {
            val request = Request.Builder().url(episodeUrl)
                .header("User-Agent", PLAYER_USER_AGENT)
                .header("Referer", config.domain)
                .build()
            val html = client.newCall(request).execute().use { it.body?.string().orEmpty() }
            if (html.isBlank()) return@withContext emptyList()
            val doc = Jsoup.parse(html, episodeUrl)

            fun addMirror(label: String, source: String, priority: ServerPriority) {
                if (source.isBlank() || source.startsWith("about:") || !addedUrls.add(source)) return
                streams += StreamData(
                    serverName = label,
                    streamUrl = source,
                    isIframe = !DirectMediaResolver.isDirectMediaUrl(source),
                    resolution = resolutionFor(label),
                    priority = priority,
                    additionalHeaders = mapOf("Referer" to episodeUrl),
                    providerName = sourceName
                )
            }

            // Anichin server buttons carry base64 HTML in data-hash. Decode the
            // embed URL directly; no provider page is opened in the player.
            val optionSelector = config.selectors.streamOption
            if (optionSelector.isNotBlank()) {
                for (option in doc.select(optionSelector)) {
                    val encoded = option.attr("data-hash")
                    val source = decodeMirrorHash(encoded, episodeUrl)
                    addMirror(option.text().trim().ifBlank { "Anichin mirror" }, source.orEmpty(), ServerPriority.HIGH)
                }
            }

            val videoSelector = config.selectors.streamVideo.ifBlank { "video source[src], video[src]" }
            for (video in doc.select(videoSelector)) {
                addMirror("Anichin Direct", video.attr("abs:src").ifBlank { video.attr("src") }, ServerPriority.HIGH)
            }

            val iframeSelector = config.selectors.streamIframe.ifBlank { "iframe[src], iframe[data-src]" }
            for (iframe in doc.select(iframeSelector)) {
                addMirror(
                    "Anichin mirror ${streams.size + 1}",
                    iframe.attr("abs:src").ifBlank { iframe.attr("abs:data-src") },
                    ServerPriority.MEDIUM
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }

    private fun decodeMirrorHash(value: String, baseUrl: String): String? {
        if (value.isBlank()) return null
        return try {
            val html = String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
            Jsoup.parse(html, baseUrl)
                .selectFirst("iframe[src], video[src], video source[src], source[src]")
                ?.attr("abs:src")
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolutionFor(value: String): StreamResolution = when {
        value.contains("1080", true) -> StreamResolution.HD_1080p
        value.contains("720", true) -> StreamResolution.HD_720p
        value.contains("480", true) -> StreamResolution.SD_480p
        value.contains("360", true) -> StreamResolution.SD_360p
        else -> StreamResolution.UNKNOWN
    }

    private companion object {
        const val PLAYER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"
    }
}
