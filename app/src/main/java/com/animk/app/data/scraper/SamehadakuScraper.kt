package com.animk.app.data.scraper

import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.net.URI
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
            val request = Request.Builder().url(episodeUrl)
                .header("User-Agent", PLAYER_USER_AGENT)
                .header("Referer", config.domain)
                .build()
            val html = client.newCall(request).execute().use { it.body?.string().orEmpty() }
            if (html.isBlank()) return@withContext emptyList()
            val doc = Jsoup.parse(html, episodeUrl)

            fun addStream(label: String, source: String, quality: StreamResolution, priority: ServerPriority) {
                if (source.isBlank() || source.startsWith("about:") || !addedUrls.add(source)) return
                streams += StreamData(
                    serverName = label,
                    streamUrl = source,
                    isIframe = !DirectMediaResolver.isDirectMediaUrl(source),
                    resolution = quality,
                    priority = priority,
                    additionalHeaders = mapOf("Referer" to episodeUrl),
                    providerName = sourceName
                )
            }

            // Samehadaku v2 exposes every server as a WordPress AJAX button.
            // Resolve those buttons first; regular iframe markup is only a fallback.
            val optionSelector = config.selectors.streamOption
            if (optionSelector.isNotBlank()) {
                for (option in doc.select(optionSelector)) {
                    val post = option.attr("data-post").trim()
                    val number = option.attr("data-nume").trim()
                    if (post.isBlank() || number.isBlank()) continue
                    val type = option.attr("data-type").trim().ifBlank { "schtml" }
                    val label = option.text().trim().ifBlank { "Samehadaku mirror" }
                    requestAjaxPlayer(config.domain, episodeUrl, post, number, type)?.let { source ->
                        addStream(label, source, resolutionFor(label), ServerPriority.HIGH)
                    }
                }
            }

            val videoSelector = config.selectors.streamVideo.ifBlank { "video source[src], video[src]" }
            for (video in doc.select(videoSelector)) {
                val source = video.attr("abs:src").ifBlank { video.attr("src") }
                addStream("Samehadaku Direct", source, resolutionFor(source), ServerPriority.HIGH)
            }

            val iframeSelector = config.selectors.streamIframe.ifBlank { "iframe[src], iframe[data-src]" }
            for (iframe in doc.select(iframeSelector)) {
                val source = iframe.attr("abs:src").ifBlank { iframe.attr("abs:data-src") }
                addStream("Samehadaku mirror ${streams.size + 1}", source, resolutionFor(source), ServerPriority.MEDIUM)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }

    private fun requestAjaxPlayer(
        domain: String,
        episodeUrl: String,
        post: String,
        number: String,
        type: String
    ): String? = try {
        val origin = URI(domain).let { "${it.scheme}://${it.host}" }
        val body = FormBody.Builder()
            .add("action", "player_ajax")
            .add("post", post)
            .add("nume", number)
            .add("type", type)
            .build()
        val request = Request.Builder().url("$origin/wp-admin/admin-ajax.php")
            .header("User-Agent", PLAYER_USER_AGENT)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Origin", origin)
            .header("Referer", episodeUrl)
            .post(body)
            .build()
        val response = client.newCall(request).execute().use { it.body?.string().orEmpty() }
        Jsoup.parse(response, origin)
            .selectFirst("iframe[src], video[src], video source[src], source[src]")
            ?.attr("abs:src")
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
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
