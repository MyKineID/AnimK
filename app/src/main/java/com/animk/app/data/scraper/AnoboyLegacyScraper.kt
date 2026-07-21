package com.animk.app.data.scraper

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
import java.net.URLEncoder

/**
 * Legacy Anoboy keeps its current episode list in the first `singlelink` block.
 * Parsing only that block prevents remakes, movies, and download bundles from
 * being mixed into the main series episode list.
 */
class AnoboyLegacyScraper : BaseScraper {
    override val sourceName: String = "Anoboy Legacy"
    override val sourceKey: String = "anoboy_legacy"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val doc = Jsoup.parse(response.body?.string().orEmpty(), response.request.url.toString())
                doc.select("a[rel=bookmark]").mapNotNull { card ->
                    val link = card.attr("abs:href")
                    val title = cleanProviderTitle(card.selectFirst("h3.ibox1")?.text().orEmpty())
                    if (link.isBlank() || title.isBlank()) return@mapNotNull null
                    val image = card.selectFirst("img")
                    val poster = image?.attr("abs:src").orEmpty()
                    MediaItem(
                        id = link,
                        title = title,
                        type = MediaType.ANIME,
                        posterUrl = poster,
                        backdropUrl = poster.ifBlank { null },
                        description = "Anoboy Legacy stream source"
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
                val doc = Jsoup.parse(response.body?.string().orEmpty(), response.request.url.toString())
                val currentEpisodeBlock = doc.select("div.singlelink")
                    .firstOrNull { it.selectFirst("ul.lcp_catlist a[href]") != null }
                    ?: return@withContext emptyList()
                var fallbackNumber = 1f
                currentEpisodeBlock.select("ul.lcp_catlist a[href]").mapNotNull { link ->
                    val title = cleanProviderTitle(link.text())
                    if (title.contains("sampai", ignoreCase = true) || title.contains("download", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    val url = link.attr("abs:href")
                    if (url.isBlank()) return@mapNotNull null
                    val number = episodeNumberFromText(title) ?: fallbackNumber
                    fallbackNumber += 1f
                    Episode(url, url, number, title.ifBlank { "Episode ${number.toInt()}" })
                }.distinctBy { it.sourceUrl }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(episodeUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", config.domain)
                .build()
            client.newCall(request).execute().use { response ->
                val doc = Jsoup.parse(response.body?.string().orEmpty(), response.request.url.toString())
                val candidates = buildList {
                    doc.select("iframe#mediaplayer[src], iframe[src]").forEach { frame ->
                        add("Blogger" to frame.attr("abs:src"))
                    }
                    doc.select("a[data-video]").forEach { button ->
                        add(button.text().ifBlank { "Mirror" } to button.attr("abs:data-video"))
                    }
                }.filter { (_, url) -> url.startsWith("http") }.distinctBy { it.second }
                candidates.map { (label, url) ->
                    StreamData(
                        serverName = "Anoboy Legacy · $label",
                        streamUrl = url,
                        isIframe = !DirectMediaResolver.isDirectMediaUrl(url),
                        resolution = resolutionFor(label),
                        priority = ServerPriority.HIGH,
                        additionalHeaders = mapOf("Referer" to episodeUrl),
                        providerName = sourceName
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun resolutionFor(value: String): StreamResolution = when {
        value.contains("1080") -> StreamResolution.HD_1080p
        value.contains("720") -> StreamResolution.HD_720p
        value.contains("480") -> StreamResolution.SD_480p
        value.contains("360") -> StreamResolution.SD_360p
        else -> StreamResolution.UNKNOWN
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"
    }
}
