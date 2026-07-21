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
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        val sourceUrls = mutableSetOf<String>()
        val directUrls = mutableSetOf<String>()
        try {
            val req = Request.Builder().url(episodeUrl)
                .header("User-Agent", PLAYER_USER_AGENT).build()
            val html = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)

            fun addDirectStream(sourceUrl: String, serverName: String, resolution: StreamResolution) {
                if (!sourceUrls.add(sourceUrl)) return
                val resolvedUrl = resolvePlayableUrl(sourceUrl)
                if (isDirectVideoUrl(resolvedUrl) && directUrls.add(resolvedUrl)) {
                    streams.add(StreamData(
                        serverName = serverName,
                        streamUrl = resolvedUrl,
                        isIframe = false,
                        resolution = resolution,
                        priority = ServerPriority.HIGH
                    ))
                }
            }

            // The mirror menu is loaded through WordPress AJAX. Resolve its DesuStream
            // entries (480p/720p) into direct media, rather than displaying iframe pages.
            val mirrors = parseMirrorOptions(doc)
                .filter { it.server.contains("ondesu", ignoreCase = true) }
                .sortedByDescending { qualityHeight(it.quality) }
            val nonce = if (mirrors.isNotEmpty()) fetchMirrorNonce(config.domain, episodeUrl) else null
            if (nonce != null) {
                for (mirror in mirrors) {
                    fetchMirrorIframe(config.domain, episodeUrl, nonce, mirror)?.let { sourceUrl ->
                        addDirectStream(sourceUrl, "DesuStream ${mirror.server.trim()} ${mirror.quality}", resolutionFor(mirror.quality))
                    }
                }
            }

            // Fallback for pages that have no mirror menu or an unavailable mirror action.
            val iframeSel = config.selectors.streamIframe.ifBlank { "iframe[src]" }
            for (iframe in doc.select(iframeSel)) {
                val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.startsWith("about:")) {
                    addDirectStream(
                        sourceUrl = src,
                        serverName = if (src.contains("desustream", true)) "DesuStream Default" else "Server ${streams.size + 1}",
                        resolution = resolutionFor(src)
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        streams
    }

    private data class MirrorOption(
        val id: Int,
        val index: Int,
        val quality: String,
        val server: String
    )

    private fun parseMirrorOptions(doc: Document): List<MirrorOption> = doc.select("[data-content]").mapNotNull { element ->
        try {
            val decoded = String(Base64.decode(element.attr("data-content"), Base64.DEFAULT), Charsets.UTF_8)
            val data = json.parseToJsonElement(decoded) as? JsonObject ?: return@mapNotNull null
            MirrorOption(
                id = data["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null,
                index = data["i"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null,
                quality = data["q"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                server = element.text().trim().ifBlank { "Mirror" }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchMirrorNonce(domain: String, referer: String): String? = try {
        val endpoint = "${domain.trimEnd('/')}/wp-admin/admin-ajax.php"
        val request = Request.Builder().url(endpoint)
            .header("User-Agent", PLAYER_USER_AGENT)
            .header("Referer", referer)
            .post(FormBody.Builder().add("action", MIRROR_NONCE_ACTION).build())
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            (json.parseToJsonElement(body) as? JsonObject)?.get("data")?.jsonPrimitive?.content
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchMirrorIframe(
        domain: String,
        referer: String,
        nonce: String,
        mirror: MirrorOption
    ): String? = try {
        val endpoint = "${domain.trimEnd('/')}/wp-admin/admin-ajax.php"
        val body = FormBody.Builder()
            .add("id", mirror.id.toString())
            .add("i", mirror.index.toString())
            .add("q", mirror.quality)
            .add("nonce", nonce)
            .add("action", MIRROR_IFRAME_ACTION)
            .build()
        val request = Request.Builder().url(endpoint)
            .header("User-Agent", PLAYER_USER_AGENT)
            .header("Referer", referer)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val encodedHtml = (json.parseToJsonElement(response.body?.string().orEmpty()) as? JsonObject)
                ?.get("data")?.jsonPrimitive?.content ?: return null
            val mirrorHtml = String(Base64.decode(encodedHtml, Base64.DEFAULT), Charsets.UTF_8)
            Jsoup.parse(mirrorHtml, domain).selectFirst("iframe[src]")?.attr("abs:src")
        }
    } catch (_: Exception) {
        null
    }

    private fun resolutionFor(value: String): StreamResolution = when {
        qualityHeight(value) >= 1080 -> StreamResolution.HD_1080p
        qualityHeight(value) >= 720 -> StreamResolution.HD_720p
        qualityHeight(value) >= 480 -> StreamResolution.SD_480p
        qualityHeight(value) >= 360 -> StreamResolution.SD_360p
        else -> StreamResolution.UNKNOWN
    }

    /** Resolves DesuStream → Blogger → range-enabled Google Video MP4 for Media3. */
    private fun resolvePlayableUrl(url: String): String {
        val bloggerUrl = resolveDesustreamUrl(url)
        return resolveBloggerVideoUrl(bloggerUrl) ?: bloggerUrl
    }

    private fun resolveDesustreamUrl(wrapperUrl: String): String {
        if (!wrapperUrl.contains("desustream.info", ignoreCase = true)) return wrapperUrl
        return try {
            // Wrapper URLs already contain ?id=..., so mode must be appended with '&'.
            val separator = if (wrapperUrl.contains('?')) "&" else "?"
            val jsonUrl = "$wrapperUrl${separator}mode=json&_=${System.currentTimeMillis()}"
            client.newCall(Request.Builder().url(jsonUrl)
                .header("User-Agent", PLAYER_USER_AGENT)
                .header("Referer", wrapperUrl)
                .build()).execute().use { resp ->
                val body = resp.body?.string() ?: return@use wrapperUrl
                val obj = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
                if (obj?.get("ok")?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true) {
                    obj["video"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: wrapperUrl
                } else {
                    // HD wrappers expose a native <video><source>, not the JSON endpoint.
                    Jsoup.parse(body, wrapperUrl)
                        .selectFirst("video source[src], video[src], source[src]")
                        ?.attr("abs:src")
                        ?.takeIf { it.isNotBlank() }
                        ?: wrapperUrl
                }
            }
        } catch (_: Exception) { wrapperUrl }
    }

    /** Blogger's page is not media. Its WcwnYd RPC returns signed googlevideo MP4 URLs. */
    private fun resolveBloggerVideoUrl(bloggerUrl: String): String? = try {
        val token = bloggerUrl.toHttpUrlOrNull()?.queryParameter("token")?.takeIf { it.isNotBlank() }
            ?: return null
        val argument = "[\"${token.escapeJson()}\",null,0]"
        val payload = "[[[\"WcwnYd\",\"${argument.escapeJson()}\",null,\"generic\"]]]"
        val request = Request.Builder()
            .url("https://www.blogger.com/_/BloggerVideoPlayerUi/data/batchexecute?rpcids=WcwnYd&source-path=%2Fvideo.g&rt=c")
            .header("User-Agent", PLAYER_USER_AGENT)
            .header("Accept", "*/*")
            .header("Origin", "https://www.blogger.com")
            .header("Referer", bloggerUrl)
            .header("X-Same-Domain", "1")
            .post(FormBody.Builder().add("f.req", payload).build())
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string().orEmpty()
        }.replace("\\/", "/")

        GOOGLE_VIDEO_REGEX.findAll(body)
            .map { decodeEscapedUrl(it.value) }
            .filter { isDirectVideoUrl(it) }
            .maxByOrNull { qualityHeight(it) }
    } catch (_: Exception) {
        null
    }

    private fun isDirectVideoUrl(url: String): Boolean {
        val path = url.substringBefore('?').lowercase(Locale.ROOT)
        return path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".webm") ||
            path.endsWith(".mkv") || url.contains("googlevideo.com/videoplayback", true) ||
            url.contains("mime=video%2fmp4", true) || url.contains("mime=video/mp4", true)
    }

    private fun qualityHeight(value: String): Int {
        if (!value.startsWith("http", ignoreCase = true)) {
            return value.filter(Char::isDigit).toIntOrNull() ?: 0
        }
        return when (value.toHttpUrlOrNull()?.queryParameter("itag")?.toIntOrNull()) {
            37, 137 -> 1080
            22, 136 -> 720
            35, 59, 135 -> 480
            18, 34, 134 -> 360
            else -> 0
        }
    }

    private fun decodeEscapedUrl(value: String): String {
        var result = value.trimEnd('\\')
        repeat(3) {
            result = ESCAPED_UNICODE_REGEX.replace(result) { it.groupValues[1].toInt(16).toChar().toString() }
                .replace("\\/", "/")
                .replace("\\\\", "\\")
        }
        return result.trimEnd('\\')
    }

    private fun String.escapeJson(): String = buildString(length + 8) {
        for (character in this@escapeJson) when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }

    private companion object {
        const val PLAYER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"
        const val MIRROR_NONCE_ACTION = "aa1208d27f29ca340c92c66d1926f13f"
        const val MIRROR_IFRAME_ACTION = "2a3505c93b0035d3f455df82bf976b84"
        val GOOGLE_VIDEO_REGEX = Regex("""https://[^"\s]+googlevideo\.com/videoplayback[^"\s]+""", RegexOption.IGNORE_CASE)
        val ESCAPED_UNICODE_REGEX = Regex("""\\+u([0-9a-fA-F]{4})""")
    }

    /** Extract anime title from detail page. */
    private fun extractTitle(doc: Document): String? {
        return doc.selectFirst("h1, .entry-title, .jdlflm, .infozin h2")?.text()?.trim()
            ?: doc.title().removeSuffix(" | Otaku Desu").trim()
    }
}
