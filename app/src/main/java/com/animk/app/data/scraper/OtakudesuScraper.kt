package com.animk.app.data.scraper

import android.util.Base64
import com.animk.app.data.model.*
import com.animk.app.data.network.OkHttpClientBuilder
import com.animk.app.data.remoteconfig.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.Locale

/**
 * Otakudesu scraper.
 *
 * Semua domain/selector dari remote config (VPS).
 * Search: ongoing page + fallback direct slug construction.
 * Episodes: semua link /episode/ dari halaman detail.
 * Streams: ?mode=json untuk desustream, direct video URL.
 */
class OtakudesuScraper : BaseScraper {
    override val sourceName: String = "Otakudesu"
    override val sourceKey: String = "otakudesu"
    private val client = OkHttpClientBuilder.buildUnsafeClient()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** Suffix patterns for otakudesu anime detail URLs (newest first). */
    private val urlSuffixes = listOf("-sub-indo", "-subtitle-indonesia", "")

    override suspend fun search(query: String, config: ProviderConfig): List<MediaItem> = withContext(Dispatchers.IO) {
        if (config.domain.isBlank()) return@withContext emptyList()
        val domain = config.domain.trimEnd('/')
        val results = mutableListOf<MediaItem>()

        // 1. Try ongoing page (static HTML, for currently airing anime)
        try {
            val req = Request.Builder().url("$domain/ongoing-anime/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
            val html = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)
            for (el in doc.select(".venz ul li .detpost")) {
                val title = el.selectFirst(".jdlflm")?.text()?.trim() ?: continue
                if (title.contains(query, ignoreCase = true)) {
                    results.add(MediaItem(
                        id = el.selectFirst(".thumb a[href]")?.attr("abs:href") ?: continue,
                        title = title, type = MediaType.ANIME,
                        posterUrl = el.selectFirst(".thumbz img")?.attr("abs:src") ?: "https://picsum.photos/300/450",
                        backdropUrl = null, description = "Otakudesu stream source", genres = listOf("Action", "Anime")
                    ))
                }
            }
        } catch (_: Exception) {}

        // 2. If query is empty/blank, return only ongoing results
        if (query.isBlank()) return@withContext results

        // 3. Try direct slug construction: domain/anime/{slug}{suffix}/
        if (results.isEmpty()) {
            val baseSlug = query.lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .take(60)

            for (suffix in urlSuffixes) {
                try {
                    val url = "$domain/anime/$baseSlug$suffix/"
                    val req = Request.Builder().url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
                    val resp = client.newCall(req).execute()
                    if (resp.code == 200) {
                        val detailHtml = resp.body?.string() ?: continue
                        val detailDoc = Jsoup.parse(detailHtml)
                        val title = extractTitle(detailDoc) ?: query
                        val poster = detailDoc.selectFirst("img.attachment-post-thumbnail")?.attr("abs:src") ?: ""
                        results.add(MediaItem(
                            id = url, title = title, type = MediaType.ANIME,
                            posterUrl = poster.ifEmpty { "https://picsum.photos/300/450" },
                            backdropUrl = poster, description = "Otakudesu stream source", genres = listOf("Action", "Anime")
                        ))
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        results.distinctBy { it.id }
    }

    override suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode> = withContext(Dispatchers.IO) {
        if (config.domain.isBlank()) return@withContext emptyList()
        val episodes = mutableListOf<Episode>()
        try {
            val req = Request.Builder().url(mediaUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
            val html = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // Extract ALL episode links from the detail page
            val epSel = config.selectors.episodeLink.ifBlank { "a[href*='/episode/']" }
            val seen = mutableSetOf<String>()
            var count = 1f
            for (el in doc.select(epSel)) {
                val epUrl = el.attr("abs:href")
                if (epUrl.isNotBlank() && seen.add(epUrl)) {
                    episodes.add(Episode(id = epUrl, sourceUrl = epUrl, episodeNumber = count,
                        title = el.text().ifEmpty { "Episode ${count.toInt()}" }))
                    count += 1f
                }
            }

            // Reverse so episode 1 is first
            if (episodes.size > 1) episodes.reverse()

            if (episodes.isEmpty()) {
                episodes.add(Episode(id = mediaUrl, sourceUrl = mediaUrl, episodeNumber = 1f, title = "Episode 1"))
            }
        } catch (e: Exception) { e.printStackTrace() }
        episodes
    }

    override suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamData>()
        val addedUrls = mutableSetOf<String>()
        try {
            val req = Request.Builder().url(episodeUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
            val html = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            // 1. Base64 decoded streams
            for (el in doc.select("[data-content]")) {
                val b64 = el.attr("data-content")
                if (b64.isNotBlank()) {
                    try {
                        val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
                        if (decoded.startsWith("http") && addedUrls.add(decoded)) {
                            streams.add(StreamData(
                                serverName = el.text().ifEmpty { "OtakuStream ${streams.size + 1}" },
                                streamUrl = resolveDesustreamUrl(decoded), isIframe = false,
                                resolution = StreamResolution.HD_720p, priority = ServerPriority.HIGH))
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Iframes
            val iframeSel = config.selectors.streamIframe.ifBlank { "iframe[src]" }
            for (iframe in doc.select(iframeSel)) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:") && addedUrls.add(src)) {
                    streams.add(StreamData(
                        serverName = if (src.contains("desustream", true)) "DesuStream HD" else "Server ${streams.size + 1}",
                        streamUrl = resolveDesustreamUrl(src), isIframe = false,
                        resolution = StreamResolution.HD_720p, priority = ServerPriority.HIGH))
                }
            }

            // 3. Fallback episode page
            if (addedUrls.add(episodeUrl)) {
                streams.add(StreamData(serverName = "Otakudesu Web Player", streamUrl = episodeUrl, isIframe = true,
                    resolution = StreamResolution.HD_720p, priority = ServerPriority.LOW))
            }
        } catch (e: Exception) { e.printStackTrace() }
        streams
    }

    private fun resolveDesustreamUrl(wrapperUrl: String): String {
        if (!wrapperUrl.contains("desustream.info")) return wrapperUrl
        return try {
            val resp = client.newCall(Request.Builder().url("$wrapperUrl?mode=json&_=${System.currentTimeMillis()}")
                .header("User-Agent", "Mozilla/5.0").header("Referer", wrapperUrl).build()).execute()
            val body = resp.body?.string() ?: return wrapperUrl
            val obj = json.parseToJsonElement(body) as? JsonObject
            if (obj?.get("ok")?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true) {
                obj["video"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: wrapperUrl
            } else wrapperUrl
        } catch (_: Exception) { wrapperUrl }
    }

    /** Extract anime title from detail page. */
    private fun extractTitle(doc: Document): String? {
        return doc.selectFirst("h1, .entry-title, .jdlflm, .infozin h2")?.text()?.trim()
            ?: doc.title().removeSuffix(" | Otaku Desu").trim()
    }
}
