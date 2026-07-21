package com.animk.app.data.repository

import android.util.Log
import com.animk.app.data.cache.ScraperCache
import com.animk.app.data.model.Episode
import com.animk.app.data.model.EpisodeSource
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType
import com.animk.app.data.model.StreamData
import com.animk.app.data.remoteconfig.ProviderConfig
import com.animk.app.data.scraper.DirectMediaResolver
import com.animk.app.data.scraper.SourceRegistry
import com.animk.app.data.remoteconfig.DirectorConfigProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.Locale

/**
 * Bridges AniList metadata → Scraper episode lists → Multi-server Stream URLs.
 *
 * Uses remote Director config to determine which providers are active
 * and in which priority order they should be tried.
 * No domain/selector is hardcoded here.
 */
class ScraperRepository {

    /**
     * Search anime title on all active (config) providers, find a match, then fetch its episode list.
     * Returns a list of real Episode objects with source URLs.
     */
    suspend fun fetchEpisodesForTitle(
        title: String,
        mediaType: MediaType? = null
    ): List<Episode> = withContext(Dispatchers.IO) {
        val directorVersion = DirectorConfigProvider.getConfig().version
        val cacheKey = "$title:${mediaType?.name.orEmpty()}:$directorVersion"
        ScraperCache.getEpisodes(cacheKey)?.let { return@withContext it }
        val activeProviders = DirectorConfigProvider.getActiveProviders()
            .filter { (_, config) -> mediaType == null || providerSupports(config, mediaType) }

        // Search every active provider, then merge the same episode into one row.
        // A provider can return a stale/empty best match, so try the next close
        // result instead of concluding that the title has no episodes.
        val providerEpisodes = coroutineScope {
            activeProviders.map { (key, config) ->
                async {
                    val scraper = SourceRegistry.getSource(key, config) ?: return@async emptyList()
                    try {
                        val candidates = linkedMapOf<String, MediaItem>()
                        val attemptedIds = mutableSetOf<String>()
                        for (query in titleSearchVariants(title)) {
                            scraper.search(query, config).forEach { candidate ->
                                candidates.putIfAbsent(candidate.id, candidate)
                            }
                            val matches = candidates.values
                                .filter { attemptedIds.add(it.id) }
                                .map { it to titleMatchScore(title, it.title) }
                                .filter { (_, score) -> score >= MIN_TITLE_MATCH }
                                .sortedByDescending { (_, score) -> score }

                            for ((match, _) in matches) {
                                val providerEpisodes = scraper.getEpisodes(match.id, config)
                                if (providerEpisodes.isEmpty()) continue
                                Log.i(LOG_TAG, "$key matched ${match.title}: ${providerEpisodes.size} episode(s)")
                                return@async providerEpisodes.map { episode ->
                                    val number = episodeNumberFrom(episode)
                                    episode.copy(
                                        episodeNumber = number,
                                        sources = listOf(EpisodeSource(key, scraper.sourceName, episode.sourceUrl))
                                    )
                                }
                            }
                        }
                        Log.i(LOG_TAG, "$key has no usable episode match for $title")
                        emptyList()
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "$key failed to fetch episodes for $title", e)
                        emptyList()
                    }
                }
            }.awaitAll()
        }

        mergeEpisodes(providerEpisodes.flatten()).also { episodes ->
            Log.i(LOG_TAG, "merged ${episodes.size} episode(s) from ${providerEpisodes.count { it.isNotEmpty() }} provider(s) for $title")
            if (episodes.isNotEmpty()) ScraperCache.putEpisodes(cacheKey, episodes)
        }
    }

    /** Backward-compatible single-source entry point. */
    suspend fun fetchStreamsForEpisode(episodeUrl: String, animeTitle: String = ""): List<StreamData> {
        if (!episodeUrl.startsWith("http")) return emptyList()
        val provider = DirectorConfigProvider.getActiveProviders()
            .firstOrNull { (_, config) -> belongsToProvider(episodeUrl, config) }
        return fetchStreamsForEpisodes(
            listOf(EpisodeSource(provider?.first.orEmpty(), provider?.first.orEmpty(), episodeUrl)),
            animeTitle
        )
    }

    /**
     * Resolves all provider pages that contain the selected episode. Unsupported
     * iframe pages are excluded; only URLs playable by Media3 reach the player.
     */
    suspend fun fetchStreamsForEpisodes(
        episodeSources: List<EpisodeSource>,
        animeTitle: String = ""
    ): List<StreamData> = withContext(Dispatchers.IO) {
        val sources = episodeSources
            .filter { it.sourceUrl.startsWith("http") }
            .distinctBy { it.sourceUrl }
        if (sources.isEmpty()) return@withContext emptyList()

        val cacheKey = "episode_sources_v2:${sources.joinToString("|") { it.sourceUrl }}"
        ScraperCache.getStreams(cacheKey)?.let { return@withContext it }
        val providers = DirectorConfigProvider.getActiveProviders().toMap()

        val streamsBySource = coroutineScope {
            sources.map { source ->
                async {
                    val matched = providers.entries.firstOrNull { (key, config) ->
                        key == source.providerKey || belongsToProvider(source.sourceUrl, config)
                    } ?: return@async emptyList()
                    val scraper = SourceRegistry.getSource(matched.key, matched.value) ?: return@async emptyList()
                    try {
                        val rawStreams = scraper.getStreams(source.sourceUrl, matched.value)
                        Log.i(LOG_TAG, "${matched.key} exposed ${rawStreams.size} raw server(s)")
                        coroutineScope {
                            rawStreams.map { stream ->
                                async {
                                    val resolved = DirectMediaResolver.resolve(stream.streamUrl, stream.additionalHeaders)
                                        ?: return@async null
                                    stream.copy(
                                        streamUrl = resolved.url,
                                        isIframe = false,
                                        additionalHeaders = resolved.additionalHeaders,
                                        providerName = source.providerName.ifBlank { scraper.sourceName }
                                    )
                                }
                            }.awaitAll().filterNotNull()
                        }
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "${matched.key} failed to resolve stream page", e)
                        emptyList()
                    }
                }
            }.awaitAll()
        }

        val deduplicated = mutableSetOf<String>()
        streamsBySource.flatten()
            .filter { deduplicated.add(it.streamUrl) }
            .sortedBy { it.priority.ordinal }
            .also { streams ->
                Log.i(LOG_TAG, "resolved ${streams.size} native stream(s) from ${sources.size} episode source(s)")
                if (streams.isNotEmpty()) ScraperCache.putStreams(cacheKey, streams)
            }
    }

    private fun providerSupports(config: ProviderConfig, mediaType: MediaType): Boolean =
        config.mediaTypes.isEmpty() || config.mediaTypes.any { it.equals(mediaType.name, ignoreCase = true) }

    private fun belongsToProvider(url: String, config: ProviderConfig): Boolean = try {
        val episodeHost = URI(url).host?.removePrefix("www.")?.lowercase()
        if (episodeHost == null) false else {
            (listOf(config.domain) + config.aliases)
                .mapNotNull { candidate -> URI(candidate).host?.removePrefix("www.")?.lowercase() }
                .any { providerHost -> episodeHost == providerHost || episodeHost.endsWith(".$providerHost") }
        }
    } catch (_: Exception) {
        false
    }

    private fun mergeEpisodes(episodes: List<Episode>): List<Episode> = episodes
        .groupBy { "%.3f".format(Locale.US, episodeNumberFrom(it)) }
        .values
        .map { group ->
            val primary = group.first()
            val sources = group.flatMap { episode ->
                episode.sources.ifEmpty {
                    listOf(EpisodeSource("", "", episode.sourceUrl))
                }
            }.distinctBy { it.sourceUrl }
            primary.copy(
                sourceUrl = sources.first().sourceUrl,
                episodeNumber = episodeNumberFrom(primary),
                sources = sources,
                description = if (sources.size > 1) "Tersedia dari ${sources.size} provider" else primary.description
            )
        }
        .sortedBy { it.episodeNumber }

    private fun episodeNumberFrom(episode: Episode): Float {
        val parsed = EPISODE_NUMBER_REGEX.find(episode.title)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        return parsed ?: episode.episodeNumber
    }

    private fun titleSearchVariants(title: String): List<String> {
        val normalized = title.replace(Regex("\\s+"), " ").trim()
        val withoutSubtitle = normalized
            .replace(Regex("\\s+(?:subtitle|sub)\\s+(?:indo(?:nesia)?|english)\\b.*", RegexOption.IGNORE_CASE), "")
            .trim()
        val beforeColon = withoutSubtitle.substringBefore(':').trim()
        return listOf(normalized, withoutSubtitle, beforeColon)
            .filter { it.length >= 2 }
            .distinct()
            .take(3)
    }

    private fun titleMatchScore(query: String, candidate: String): Float {
        val wanted = titleTokens(query)
        val found = titleTokens(candidate)
        if (wanted.isEmpty() || found.isEmpty()) return 0f
        if (wanted == found) return 1f
        return wanted.intersect(found).size.toFloat() / wanted.size
    }

    private fun titleTokens(value: String): Set<String> = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .split(' ')
        .filter { it.length > 1 && it !in TITLE_STOP_WORDS }
        .toSet()

    private fun deduplicateMedia(items: List<MediaItem>): List<MediaItem> =
        items.distinctBy { item ->
            item.title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()
        }

    private companion object {
        const val LOG_TAG = "AnimKScraper"
        const val MIN_TITLE_MATCH = 0.45f
        val EPISODE_NUMBER_REGEX = Regex("(?:episode|eps?|e)\\s*[-_. ]*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val TITLE_STOP_WORDS = setOf("anime", "sub", "subtitle", "indo", "indonesia", "season", "movie")
    }

    // ── Convenience methods for backward compatibility ──────────

    /** Search across all active providers. Used by SearchScreen. */
    suspend fun searchAll(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "search_all_v3:$query"
        ScraperCache.getCatalog(cacheKey)?.let { return@withContext it }
        val activeProviders = DirectorConfigProvider.getActiveProviders()
        val results = coroutineScope {
            activeProviders.map { (key, config) ->
                async {
                    val scraper = SourceRegistry.getSource(key, config) ?: return@async emptyList()
                    runCatching { scraper.search(query, config) }.getOrElse {
                        it.printStackTrace(); emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
        deduplicateMedia(results).also { ScraperCache.putCatalog(cacheKey, it) }
    }

    /** Search a category without needlessly querying providers for other media. */
    suspend fun searchByType(query: String, type: MediaType): List<MediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "search_type_v4:${type.name}:$query"
        ScraperCache.getCatalog(cacheKey)?.let { return@withContext it }
        val providers = DirectorConfigProvider.getActiveProviders()
            .filter { (_, config) -> providerSupports(config, type) }
        val results = coroutineScope {
            providers.map { (key, config) ->
                async {
                    val scraper = SourceRegistry.getSource(key, config) ?: return@async emptyList()
                    runCatching { scraper.search(query, config) }.getOrElse { emptyList() }
                }
            }.awaitAll().flatten()
        }
        deduplicateMedia(results).filter { it.type == type }
            .also { ScraperCache.putCatalog(cacheKey, it) }
    }

    /**
     * Fetch ongoing/latest anime from all active providers.
     * Uses each provider's ongoing/popular page to get currently airing titles.
     */
    suspend fun fetchOngoing(limit: Int = 20): List<MediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "ongoing_v4:$limit"
        ScraperCache.getCatalog(cacheKey)?.let { return@withContext it }
        val activeProviders = DirectorConfigProvider.getActiveProviders()
            .filter { (_, config) -> providerSupports(config, MediaType.ANIME) }
        val results = coroutineScope {
            activeProviders.map { (key, config) ->
                async {
                    val scraper = SourceRegistry.getSource(key, config) ?: return@async emptyList()
                    runCatching { scraper.search("", config) }.getOrElse {
                        it.printStackTrace(); emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
        deduplicateMedia(results).take(limit).also { ScraperCache.putCatalog(cacheKey, it) }
    }

    /**
     * Check if a title has episodes available from any active provider.
     */
    suspend fun hasEpisodes(title: String): Boolean = withContext(Dispatchers.IO) {
        val activeProviders = DirectorConfigProvider.getActiveProviders()
        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key, config) ?: continue
            try {
                val searchResults = scraper.search(title, config)
                if (searchResults.isNotEmpty()) {
                    val episodes = scraper.getEpisodes(searchResults.first().id, config)
                    if (episodes.isNotEmpty()) return@withContext true
                }
            } catch (_: Exception) {}
        }
        false
    }
}
