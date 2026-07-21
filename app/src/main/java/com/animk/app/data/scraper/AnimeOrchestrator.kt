package com.animk.app.data.scraper

import com.animk.app.data.model.StreamData
import com.animk.app.data.remoteconfig.DirectorConfigProvider

/**
 * Waterfall stream resolver.
 * Given an episode page URL, tries each active provider (from remote config)
 * in priority order until one returns streams.
 * No hardcoded domains or selectors.
 */
class AnimeOrchestrator {

    /**
     * Given an actual episode page URL (not a title!), try each active provider's
     * getStreams() in priority order until one returns results.
     */
    suspend fun getStreamsWaterfall(episodeUrl: String): List<StreamData> {
        val activeProviders = DirectorConfigProvider.getActiveProviders()

        for ((key, config) in activeProviders) {
            val scraper = SourceRegistry.getSource(key, config) ?: continue
            try {
                val streams = scraper.getStreams(episodeUrl, config)
                if (streams.isNotEmpty()) return streams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }
}
