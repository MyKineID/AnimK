package com.animk.app.data.scraper

import android.util.Base64
import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType
import com.animk.app.data.model.ServerPriority
import com.animk.app.data.model.StreamData
import com.animk.app.data.model.StreamResolution
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

/**
 * Selector-driven scraper used for Director-configured fallback providers.
 *
 * It intentionally returns only raw video/iframe candidates. They still pass
 * through [DirectMediaResolver] in ScraperRepository, so an HTML provider page
 * can never be opened by the native player.
 */
class GenericProviderScraper(
    private val providerKey: String,
    private val configuredName: String,
    private val configuredType: MediaType
) : BaseScraper {
    override val sourceKey: String = providerKey
    override val sourceName: String = configuredName.ifBlank {
        providerKey.replace('-', ' ').replaceFirstChar { it.uppercase() }
    }

    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        if (config.domain.isBlank()) return@withContext emptyList()
        try {
            val requestUrl = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(requestUrl).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) return@withContext emptyList()
                val document = Jsoup.parse(html, response.request.url.toString())
                val selectors = config.selectors
                val listSelector = selectors.list.ifBlank { "article, .bs, .item" }
                val titleSelector = selectors.title.ifBlank { "h2, h3, .tt, .title" }
                val linkSelector = selectors.link.ifBlank { "a[href]" }
                val imageSelector = selectors.image.ifBlank { "img" }

                document.select(listSelector).mapNotNull { card ->
                    val linkElement = card.selectFirst(linkSelector) ?: card.takeIf { it.`is`("a[href]") }
                    val link = linkElement?.attr("abs:href").orEmpty()
                    val title = cleanProviderTitle(card.selectFirst(titleSelector)?.text().orEmpty())
                    if (link.isBlank() || title.isBlank()) return@mapNotNull null
                    val image = card.selectFirst(imageSelector)
                    val poster = image?.attr("abs:data-original").orEmpty()
                        .ifBlank { image?.attr("abs:data-src").orEmpty() }
                        .ifBlank { image?.attr("abs:src").orEmpty() }
                        .ifBlank { image?.attr("data-setbg").orEmpty() }
                    MediaItem(
                        id = link,
                        title = title,
                        type = mediaTypeFor(config),
                        posterUrl = poster,
                        backdropUrl = poster.ifBlank { null },
                        description = "$sourceName stream source"
                    )
                }.distinctBy { it.id }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(mediaUrl).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) return@withContext emptyList()
                val document = Jsoup.parse(html, response.request.url.toString())
                val selector = config.selectors.episodeLink.ifBlank { ".eplister a[href], .episodelist a[href]" }
                var fallbackNumber = 1f
                document.select(selector).mapNotNull { element ->
                    val url = element.attr("abs:href").ifBlank { absoluteUrl(element.attr("value"), mediaUrl) }
                    if (!url.startsWith("http")) return@mapNotNull null
                    val label = cleanProviderTitle(element.text())
                    val number = element.attr("data-number").toFloatOrNull()
                        ?: element.attr("data-episode").toFloatOrNull()
                        ?: episodeNumberFromText(label)
                        ?: fallbackNumber
                    fallbackNumber += 1f
                    Episode(
                        id = url,
                        sourceUrl = url,
                        episodeNumber = number,
                        title = label.ifBlank { "Episode ${number.toInt()}" }
                    )
                }.distinctBy { it.sourceUrl }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamData>()
        val seen = mutableSetOf<String>()
        try {
            val request = Request.Builder().url(episodeUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", config.domain)
                .build()
            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) return@withContext emptyList()
                val document = Jsoup.parse(html, response.request.url.toString())

                fun addCandidate(label: String, candidate: String, priority: ServerPriority) {
                    val url = candidate.trim()
                    if (!url.startsWith("http") || !seen.add(url)) return
                    streams += StreamData(
                        serverName = label.ifBlank { "$sourceName server ${streams.size + 1}" },
                        streamUrl = url,
                        isIframe = !DirectMediaResolver.isDirectMediaUrl(url),
                        resolution = resolutionFor(label),
                        priority = priority,
                        additionalHeaders = mapOf("Referer" to episodeUrl),
                        providerName = sourceName
                    )
                }

                val videoSelector = config.selectors.streamVideo.ifBlank { "video source[src], video[src], source[src]" }
                document.select(videoSelector).forEach { element ->
                    addCandidate(
                        label = "$sourceName Direct",
                        candidate = element.attr("abs:src")
                            .ifBlank { element.attr("abs:data-src") }
                            .ifBlank { element.attr("abs:content") }
                            .ifBlank { element.attr("content") },
                        priority = ServerPriority.HIGH
                    )
                }

                val optionSelector = config.selectors.streamOption
                if (optionSelector.isNotBlank()) {
                    document.select(optionSelector).forEach { option ->
                        val label = cleanProviderTitle(option.text())
                        val fromHash = decodeEmbed(
                            option.attr("data-hash").ifBlank { option.attr("value") },
                            episodeUrl
                        )
                        val candidate = fromHash
                            ?: option.attr("abs:data-url").ifBlank { option.attr("abs:data-src") }
                            .ifBlank { option.attr("abs:data-embed") }
                            .ifBlank { option.attr("abs:data-video") }
                            .ifBlank { option.attr("abs:href") }
                            .ifBlank { absoluteUrl(option.attr("value"), episodeUrl) }
                        addCandidate(label, candidate, ServerPriority.HIGH)
                    }
                }

                val iframeSelector = config.selectors.streamIframe.ifBlank {
                    "iframe[src], iframe[data-src], iframe[data-litespeed-src]"
                }
                document.select(iframeSelector).forEach { frame ->
                    val candidate = frame.attr("abs:src")
                        .ifBlank { frame.attr("abs:data-src") }
                        .ifBlank { frame.attr("abs:data-litespeed-src") }
                    addCandidate("$sourceName mirror", candidate, ServerPriority.MEDIUM)
                }
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        streams
    }

    private fun decodeEmbed(encoded: String, baseUrl: String): String? {
        if (encoded.isBlank()) return null
        return try {
            val html = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            Jsoup.parse(html, baseUrl)
                .selectFirst("iframe[src], iframe[data-src], video[src], video source[src], source[src]")
                ?.let { element ->
                    element.attr("abs:src").ifBlank { element.attr("abs:data-src") }
                }
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun mediaTypeFor(config: ProviderConfig): MediaType =
        config.mediaTypes.firstNotNullOfOrNull { value ->
            MediaType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        } ?: configuredType

    private fun absoluteUrl(value: String, baseUrl: String): String = try {
        if (value.isBlank()) "" else URI(baseUrl).resolve(value).toString()
    } catch (_: Exception) {
        value
    }

    private fun resolutionFor(value: String): StreamResolution = when {
        value.contains("1080", ignoreCase = true) || value.contains("4k", ignoreCase = true) -> StreamResolution.HD_1080p
        value.contains("720", ignoreCase = true) -> StreamResolution.HD_720p
        value.contains("480", ignoreCase = true) -> StreamResolution.SD_480p
        value.contains("360", ignoreCase = true) -> StreamResolution.SD_360p
        else -> StreamResolution.UNKNOWN
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"
    }
}
