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
import org.json.JSONArray
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.net.URLDecoder

/** Nimegami stores every episode's server list as base64 JSON in the detail page. */
class NimegamiScraper : BaseScraper {
    override val sourceName: String = "Nimegami"
    override val sourceKey: String = "nimegami"
    private val client = OkHttpClientBuilder.buildUnsafeClient()

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val url = config.domain.trimEnd('/') + config.searchPath + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                val doc = Jsoup.parse(html, response.request.url.toString())
                doc.select(config.selectors.list.ifBlank { ".archive article" }).mapNotNull { card ->
                    val link = card.selectFirst(config.selectors.link.ifBlank { "h2 a[href]" })
                        ?.attr("abs:href").orEmpty()
                    val title = cleanProviderTitle(card.selectFirst(config.selectors.title.ifBlank { "h2 a" })?.text().orEmpty())
                    if (link.isBlank() || title.isBlank()) return@mapNotNull null
                    val image = card.selectFirst(config.selectors.image.ifBlank { ".thumbnail img" })
                    val poster = image?.attr("abs:data-src").orEmpty()
                        .ifBlank { image?.attr("abs:src").orEmpty() }
                    MediaItem(
                        id = link,
                        title = title,
                        type = MediaType.ANIME,
                        posterUrl = poster,
                        backdropUrl = poster.ifBlank { null },
                        description = "Nimegami stream source"
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
                val doc = Jsoup.parse(html, response.request.url.toString())
                var fallbackNumber = 1f
                doc.select(config.selectors.episodeLink.ifBlank { ".list_eps_stream li.select-eps[data]" })
                    .mapNotNull { element ->
                        val payload = element.attr("data").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val title = cleanProviderTitle(element.attr("title").ifBlank { element.text() })
                        val number = episodeNumberFromText(title) ?: fallbackNumber
                        fallbackNumber += 1f
                        Episode(
                            id = "$mediaUrl#nime_data=${URLEncoder.encode(payload, "UTF-8")}",
                            sourceUrl = "$mediaUrl#nime_data=${URLEncoder.encode(payload, "UTF-8")}",
                            episodeNumber = number,
                            title = title.ifBlank { "Episode ${number.toInt()}" }
                        )
                    }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        val encodedPayload = episodeUrl.substringAfter("#nime_data=", "")
        if (encodedPayload.isBlank()) return@withContext emptyList()
        val referer = episodeUrl.substringBefore('#')
        try {
            val payload = URLDecoder.decode(encodedPayload, "UTF-8")
            val values = JSONArray(String(Base64.decode(payload, Base64.DEFAULT), Charsets.UTF_8))
            buildList {
                for (index in 0 until values.length()) {
                    val item = values.optJSONObject(index) ?: continue
                    val url = item.optJSONArray("url")?.optString(0).orEmpty()
                    if (!url.startsWith("http")) continue
                    val format = item.optString("format").ifBlank { "Auto" }
                    add(
                        StreamData(
                            serverName = "Nimegami · $format",
                            streamUrl = url,
                            isIframe = !DirectMediaResolver.isDirectMediaUrl(url),
                            resolution = resolutionFor(format),
                            priority = ServerPriority.HIGH,
                            additionalHeaders = mapOf("Referer" to referer),
                            providerName = sourceName
                        )
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
