package com.animk.app.data.scraper

import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.StreamData
import com.animk.app.data.remoteconfig.ProviderConfig

/**
 * Base interface for all anime/donghua/drakor source scrapers.
 *
 * Each implementation corresponds to a provider key in the remote Director config.
 * All methods receive [ProviderConfig] from the remote config so that domain,
 * search path, and selectors are not hardcoded in the APK.
 */
/**
 * Provider result cards often contain both a title element and its parent in a
 * CSS selector. Keep only one clean title so duplicate words never become the
 * query used to find episodes.
 */
internal fun cleanProviderTitle(raw: String): String {
    val words = raw.replace(Regex("\\s+"), " ").trim().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    for (chunkSize in 1..(words.size / 2)) {
        if (words.size % chunkSize != 0) continue
        val chunks = words.chunked(chunkSize)
        if (chunks.size > 1 && chunks.drop(1).all { chunk ->
                chunk.joinToString(" ").equals(chunks.first().joinToString(" "), ignoreCase = true)
            }) {
            return chunks.first().joinToString(" ")
        }
    }
    return words.joinToString(" ")
}

/** Extract an explicit episode label without inventing a sequential number. */
internal fun episodeNumberFromText(value: String): Float? =
    Regex("(?:episode|eps?|ep|e)\\s*[-_. ]*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        .find(value)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        ?: Regex("\\b(\\d+(?:\\.\\d+)?)\\b").find(value)?.groupValues?.getOrNull(1)?.toFloatOrNull()

interface BaseScraper {
    /** Human-readable display name (e.g. "Otakudesu") */
    val sourceName: String

    /** Provider key matching remote config (e.g. "otakudesu", "kuramanime") */
    val sourceKey: String

    /** Search for media by title using the given [config]. */
    suspend fun search(query: String, config: ProviderConfig): List<MediaItem>

    /** Fetch episode list for a media detail/page URL using the given [config]. */
    suspend fun getEpisodes(mediaUrl: String, config: ProviderConfig): List<Episode>

    /** Extract available stream URLs for an episode page URL using the given [config]. */
    suspend fun getStreams(episodeUrl: String, config: ProviderConfig): List<StreamData>
}
