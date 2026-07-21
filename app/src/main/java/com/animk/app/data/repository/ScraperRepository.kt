package com.animk.app.data.repository

import com.animk.app.data.model.Episode
import com.animk.app.data.model.StreamData
import com.animk.app.data.scraper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridges AniList metadata → Scraper episode lists → Stream URLs.
 * Searches anime title across all scrapers and returns real episodes.
 */
class ScraperRepository {
    private val kurama = KuramanimeScraper()
    private val samehadaku = SamehadakuScraper()
    private val otakudesu = OtakudesuScraper()
    private val donghua = DonghuaScraper()
    private val drakor = DrakorScraper()
    private val orchestrator = AnimeOrchestrator(kurama, samehadaku, otakudesu)

    /**
     * Search anime title on all scrapers, find a match, then fetch its episode list.
     * Returns a list of real Episode objects with source URLs.
     */
    suspend fun fetchEpisodesForTitle(title: String): List<Episode> = withContext(Dispatchers.IO) {
        // Try each scraper in priority order
        val scrapers = listOf(kurama, samehadaku, otakudesu, donghua, drakor)

        for (scraper in scrapers) {
            try {
                val searchResults = scraper.search(title)
                if (searchResults.isNotEmpty()) {
                    // Pick the best match (first result)
                    val bestMatch = searchResults.first()
                    val episodes = scraper.getEpisodes(bestMatch.id)
                    if (episodes.isNotEmpty() && episodes.size > 1) {
                        return@withContext episodes
                    }
                    // If only 1 dummy episode, try next scraper
                    if (episodes.size == 1 && episodes[0].sourceUrl == bestMatch.id) {
                        continue
                    }
                    return@withContext episodes
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }

        // Fallback: return empty (UI will show single Play button)
        emptyList()
    }

    /**
     * Get stream URLs for a specific episode URL using waterfall logic.
     */
    suspend fun fetchStreamsForEpisode(episodeUrl: String): List<StreamData> = withContext(Dispatchers.IO) {
        orchestrator.getStreamsWaterfall(episodeUrl)
    }
}
