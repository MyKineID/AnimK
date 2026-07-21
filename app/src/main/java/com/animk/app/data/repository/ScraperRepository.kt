package com.animk.app.data.repository

import com.animk.app.data.model.Episode
import com.animk.app.data.model.StreamData
import com.animk.app.data.scraper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridges AniList metadata → Scraper episode lists → Multi-server Stream URLs.
 */
class ScraperRepository {
    private val kurama = KuramanimeScraper()
    private val samehadaku = SamehadakuScraper()
    private val otakudesu = OtakudesuScraper()
    private val donghua = DonghuaScraper()
    private val drakor = DrakorScraper()

    private val allScrapers: List<BaseScraper> = listOf(kurama, samehadaku, otakudesu, donghua, drakor)

    /**
     * Search anime title on all scrapers, find a match, then fetch its episode list.
     * Returns a list of real Episode objects with source URLs.
     */
    suspend fun fetchEpisodesForTitle(title: String): List<Episode> = withContext(Dispatchers.IO) {
        for (scraper in allScrapers) {
            try {
                val searchResults = scraper.search(title)
                if (searchResults.isNotEmpty()) {
                    val bestMatch = searchResults.first()
                    val episodes = scraper.getEpisodes(bestMatch.id)
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
     * Get ALL available stream URLs for an episode.
     * Tries the primary scraper for that URL, plus searches other scrapers for additional stream servers!
     */
    suspend fun fetchStreamsForEpisode(episodeUrl: String, animeTitle: String = ""): List<StreamData> = withContext(Dispatchers.IO) {
        val resultStreams = mutableListOf<StreamData>()
        val addedUrls = mutableSetOf<String>()

        // 1. First, fetch from matching scraper if episodeUrl is a direct HTTP URL
        if (episodeUrl.startsWith("http")) {
            for (scraper in allScrapers) {
                try {
                    val streams = scraper.getStreams(episodeUrl)
                    for (st in streams) {
                        if (addedUrls.add(st.streamUrl)) {
                            resultStreams.add(st)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. If resultStreams is empty or small, search animeTitle across other scrapers to get more server options
        if (animeTitle.isNotBlank() && resultStreams.size < 3) {
            for (scraper in allScrapers) {
                try {
                    val searchResults = scraper.search(animeTitle)
                    if (searchResults.isNotEmpty()) {
                        val firstMatch = searchResults.first()
                        val eps = scraper.getEpisodes(firstMatch.id)
                        val matchingEp = eps.firstOrNull() ?: Episode(id = firstMatch.id, sourceUrl = firstMatch.id, episodeNumber = 1f, title = "Ep 1")
                        val streams = scraper.getStreams(matchingEp.sourceUrl)
                        for (st in streams) {
                            if (addedUrls.add(st.streamUrl)) {
                                resultStreams.add(st)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        resultStreams
    }
}
