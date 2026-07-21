package com.animk.app.data.scraper

import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.net.URI
import java.util.Locale

/**
 * Converts supported embed pages into URLs that Media3 can play directly.
 *
 * A provider page is never returned as a playable URL. Unsupported embeds are
 * deliberately omitted instead of falling back to a WebView.
 */
data class ResolvedDirectMedia(
    val url: String,
    val additionalHeaders: Map<String, String> = emptyMap()
)

object DirectMediaResolver {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/115.0.0.0 Safari/537.36"

    private val client = OkHttpClientBuilder.buildUnsafeClient()

    suspend fun resolve(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ResolvedDirectMedia? = withContext(Dispatchers.IO) {
        resolvePage(url, headers, referer = null, depth = 0)
    }

    fun isDirectMediaUrl(url: String): Boolean {
        val path = url.substringBefore('?').lowercase(Locale.ROOT)
        return path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".webm") ||
            path.endsWith(".mkv") || url.contains("googlevideo.com/videoplayback", ignoreCase = true) ||
            url.contains("mime=video%2f", ignoreCase = true) || url.contains("mime=video/", ignoreCase = true)
    }

    private fun resolvePage(
        url: String,
        inheritedHeaders: Map<String, String>,
        referer: String?,
        depth: Int
    ): ResolvedDirectMedia? {
        if (isDirectMediaUrl(url)) return ResolvedDirectMedia(url, inheritedHeaders)
        if (depth > 1 || !url.startsWith("http", ignoreCase = true)) return null
        resolveBloggerVideo(url)?.let { direct ->
            return ResolvedDirectMedia(direct, inheritedHeaders + mapOf("Referer" to (referer ?: url)))
        }
        googleDriveFileId(url)?.let { id ->
            return ResolvedDirectMedia(
                url = "https://drive.google.com/uc?export=download&id=$id",
                additionalHeaders = inheritedHeaders + mapOf("Referer" to (referer ?: url))
            )
        }

        return try {
            val request = Request.Builder().url(url)
                .header("User-Agent", USER_AGENT)
                .apply {
                    inheritedHeaders.forEach { (name, value) -> header(name, value) }
                    referer?.let { header("Referer", it) }
                }
                .build()
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
                val mediaHeaders = inheritedHeaders + mapOf("Referer" to url)
                if (isDirectMediaUrl(finalUrl) || contentType.startsWith("video/") || contentType.contains("mpegurl")) {
                    return@use ResolvedDirectMedia(finalUrl, mediaHeaders)
                }

                val html = response.body?.string().orEmpty()
                val directUrl = extractDirectUrl(html, finalUrl)
                if (directUrl != null) {
                    return@use ResolvedDirectMedia(directUrl, mediaHeaders)
                }

                // A small number of provider pages point to one more embed page.
                // Keep this bounded so an arbitrary page can never become a crawler.
                if (depth == 0) {
                    val doc = Jsoup.parse(html, finalUrl)
                    val nestedEmbed = doc.select("iframe[src], iframe[data-src]")
                        .asSequence()
                        .map { it.attr("abs:src").ifBlank { it.attr("abs:data-src") } }
                        .firstOrNull { it.startsWith("http", ignoreCase = true) }
                    nestedEmbed?.let { resolvePage(it, inheritedHeaders, finalUrl, depth + 1) }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDirectUrl(rawHtml: String, pageUrl: String): String? {
        val html = normalizeEscapes(rawHtml)

        // Filedon embeds expose a short-lived signed MP4 URL in their hydration payload.
        DIRECT_URL_REGEX.find(html)?.value?.let { candidate ->
            val direct = normalizeCandidate(candidate, pageUrl)
            if (isDirectMediaUrl(direct)) return direct
        }

        val doc = Jsoup.parse(html, pageUrl)
        doc.select("video[src], video source[src], source[src], meta[property=og:video], meta[name=twitter:player:stream]")
            .asSequence()
            .map { element ->
                element.attr("abs:src")
                    .ifBlank { element.attr("abs:content") }
                    .ifBlank { element.attr("src") }
                    .ifBlank { element.attr("content") }
            }
            .map { normalizeCandidate(it, pageUrl) }
            .firstOrNull(::isDirectMediaUrl)
            ?.let { return it }

        PLAYER_FILE_REGEX.findAll(html)
            .map { normalizeCandidate(it.groupValues[1], pageUrl) }
            .firstOrNull(::isDirectMediaUrl)
            ?.let { return it }

        // VidHide/EarnVids use a standard P.A.C.K.E.R. script. It contains an HLS
        // source but no browser is needed to read it.
        unpackVidhide(rawHtml)?.let { unpacked ->
            HLS_VALUE_REGEX.findAll(unpacked)
                .map { normalizeCandidate(unescapeJavaScript(it.groupValues[1]), pageUrl) }
                .firstOrNull(::isDirectMediaUrl)
                ?.let { return it }
        }

        return DIRECT_URL_REGEX.findAll(html)
            .map { normalizeCandidate(it.value, pageUrl) }
            .firstOrNull(::isDirectMediaUrl)
    }

    /** Blogger's video.g URL is an HTML wrapper; WcwnYd returns native Google media. */
    private fun resolveBloggerVideo(url: String): String? = try {
        val token = url.toHttpUrlOrNull()?.queryParameter("token")?.takeIf { it.isNotBlank() } ?: return null
        val argument = "[\"${token.escapeJson()}\",null,0]"
        val payload = "[[[\"WcwnYd\",\"${argument.escapeJson()}\",null,\"generic\"]]]"
        val request = Request.Builder()
            .url("https://www.blogger.com/_/BloggerVideoPlayerUi/data/batchexecute?rpcids=WcwnYd&source-path=%2Fvideo.g&rt=c")
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://www.blogger.com")
            .header("Referer", url)
            .header("X-Same-Domain", "1")
            .post(FormBody.Builder().add("f.req", payload).build())
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string().orEmpty()
        }
        GOOGLE_VIDEO_REGEX.findAll(normalizeEscapes(body))
            .map { normalizeEscapes(it.value).trimEnd('\\') }
            .firstOrNull(::isDirectMediaUrl)
    } catch (_: Exception) {
        null
    }

    private fun String.escapeJson(): String = buildString(length + 8) {
        for (character in this@escapeJson) when (character) {
            '\\' -> append("\\\\")
            '\"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }

    private fun googleDriveFileId(url: String): String? {
        if (!url.contains("drive.google.com", ignoreCase = true)) return null
        return DRIVE_FILE_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
            ?: DRIVE_QUERY_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
    }

    private fun normalizeCandidate(value: String, pageUrl: String): String {
        val cleaned = normalizeEscapes(value)
            .trim()
            .trim('"', '\'', '\\')
            .substringBefore("&quot;")
        return try {
            URI(pageUrl).resolve(cleaned).toString()
        } catch (_: Exception) {
            cleaned
        }
    }

    private fun normalizeEscapes(value: String): String = Parser.unescapeEntities(value, false)
        .replace("\\u0026", "&")
        .replace("\\u003d", "=")
        .replace("\\u003a", ":")
        .replace("\\u002f", "/")
        .replace("\\/", "/")

    private fun unpackVidhide(html: String): String? {
        val match = PACKER_REGEX.find(html) ?: return null
        val payload = unescapeJavaScript(match.groupValues[1])
        val radix = match.groupValues[2].toIntOrNull() ?: return null
        val count = match.groupValues[3].toIntOrNull() ?: return null
        val dictionary = unescapeJavaScript(match.groupValues[4]).split('|')
        if (radix !in 2..62 || count <= 0) return null

        var unpacked = payload
        for (index in count - 1 downTo 0) {
            val replacement = dictionary.getOrNull(index).orEmpty()
            if (replacement.isNotEmpty()) {
                val token = encodeBase(index, radix)
                unpacked = unpacked.replace(Regex("\\b${Regex.escape(token)}\\b"), replacement)
            }
        }
        return unpacked
    }

    private fun unescapeJavaScript(value: String): String = value
        .replace("\\\\", "\\")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")

    private fun encodeBase(number: Int, radix: Int): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (number < radix) return alphabet[number].toString()
        return encodeBase(number / radix, radix) + alphabet[number % radix]
    }

    private val GOOGLE_VIDEO_REGEX = Regex("""https://[^"\s]+googlevideo\.com/videoplayback[^"\s]+""", RegexOption.IGNORE_CASE)
    private val PLAYER_FILE_REGEX = Regex("""file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val DRIVE_FILE_ID_REGEX = Regex("""/file/d/([A-Za-z0-9_-]+)""")
    private val DRIVE_QUERY_ID_REGEX = Regex("""[?&]id=([A-Za-z0-9_-]+)""")
    private val DIRECT_URL_REGEX = Regex(
        """https?://[^\s\"'<>\\]+?(?:\.m3u8|\.mp4|\.webm)(?:\?[^\s\"'<>\\]+)?|https?://[^\s\"'<>\\]+googlevideo\.com/videoplayback[^\s\"'<>\\]*""",
        RegexOption.IGNORE_CASE
    )
    private val HLS_VALUE_REGEX = Regex("""[\"']hls[0-9]*[\"']\s*:\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
    private val PACKER_REGEX = Regex(
        """\}\('((?:\\.|[^'])*)',(\d+),(\d+),'((?:\\.|[^'])*)'\.split\('\|'\)\)\)""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
}
