package com.animk.app.data.repository

import com.animk.app.data.model.Episode
import com.animk.app.data.model.StreamData
import com.animk.app.data.remoteconfig.ProviderConfig
import com.animk.app.data.scraper.SourceRegistry
import com.animk.app.data.remoteconfig.DirectorConfigProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

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
    suspend fun fetchEpisodesForTitle(title: String): List<Episode> = withContext(Dispatchers.IO) {
        val activeProviders = DirectorConfigProvider.getActiveProviders()

        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
            try {
                val searchResults = scraper.search(title, config)
                if (searchResults.isNotEmpty()) {
                    val bestMatch = searchResults.first()
                    val episodes = scraper.getEpisodes(bestMatch.id, config)
                    if (episodes.size > 1) {
                        return@withContext episodes
                    }
                    if (episodes.isNotEmpty()) {
                        return@withContext episodes
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emptyList()
    }

    /**
     * Get stream URLs only from the provider that owns [episodeUrl].
     *
     * Passing an Otakudesu URL to every provider made the player wait for several
     * unrelated HTTP requests and could select a different show's episode 1.
     */
    suspend fun fetchStreamsForEpisode(episodeUrl: String, animeTitle: String = ""): List<StreamData> = withContext(Dispatchers.IO) {
        if (!episodeUrl.startsWith("http")) return@withContext emptyList()

        val resultStreams = mutableListOf<StreamData>()
        val addedUrls = mutableSetOf<String>()
        val activeProviders = DirectorConfigProvider.getActiveProviders()
        val matchingProviders = activeProviders.filter { (_, config) ->
            belongsToProvider(episodeUrl, config)
        }

        for ((key, config) in matchingProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
            try {
                scraper.getStreams(episodeUrl, config).forEach { stream ->
                    if (addedUrls.add(stream.streamUrl)) resultStreams += stream
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        resultStreams.sortedBy { it.priority.ordinal }
    }

    private fun belongsToProvider(url: String, config: ProviderConfig): Boolean = try {
        val episodeHost = URI(url).host?.removePrefix("www.")?.lowercase()
        val providerHost = URI(config.domain).host?.removePrefix("www.")?.lowercase()
        episodeHost != null && providerHost != null &&
            (episodeHost == providerHost || episodeHost.endsWith(".$providerHost"))
    } catch (_: Exception) {
        false
    }

    // ── Convenience methods for backward compatibility ──────────

    /** Search across all active providers. Used by SearchScreen. */
    suspend fun searchAll(query: String): List<com.animk.app.data.model.MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<com.animk.app.data.model.MediaItem>()
        val activeProviders = DirectorConfigProvider.getActiveProviders()

        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
            try {
                results.addAll(scraper.search(query, config))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results.distinctBy { it.title }
    }

    /** Search by type (anime, donghua, drakor) using active config providers. */
    suspend fun searchByType(query: String, type: com.animk.app.data.model.MediaType): List<com.animk.app.data.model.MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<com.animk.app.data.model.MediaItem>()
        val activeProviders = DirectorConfigProvider.getActiveProviders()

        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
            try {
                val items = scraper.search(query, config)
                results.addAll(items.filter { it.type == type })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results.distinctBy { it.title }
    }

    /**
     * Fetch ongoing/latest anime from all active providers.
     * Uses each provider's ongoing/popular page to get currently airing titles.
     */
    suspend fun fetchOngoing(limit: Int = 20): List<com.animk.app.data.model.MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<com.animk.app.data.model.MediaItem>()
        val activeProviders = DirectorConfigProvider.getActiveProviders()

        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
            try {
                // Use empty or common search to get ongoing/popular from provider
                val items = scraper.search("", config)
                results.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results.distinctBy { it.title }.take(limit)
    }

    /**
     * Check if a title has episodes available from any active provider.
     */
    suspend fun hasEpisodes(title: String): Boolean = withContext(Dispatchers.IO) {
        val activeProviders = DirectorConfigProvider.getActiveProviders()
        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key) ?: continue
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
