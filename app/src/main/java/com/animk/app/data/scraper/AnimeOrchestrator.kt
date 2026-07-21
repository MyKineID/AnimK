package com.animk.app.data.scraper

import com.animk.app.data.model.StreamData

class AnimeOrchestrator(
    private val kurama: BaseScraper = KuramanimeScraper(),
    private val samehadaku: BaseScraper = SamehadakuScraper(),
    private val otakudesu: BaseScraper = OtakudesuScraper(),
    private val donghua: BaseScraper = DonghuaScraper(),
    private val drakor: BaseScraper = DrakorScraper()
) {
    /**
     * Given an actual episode page URL (not a title!), try each scraper's getStreams()
     * in waterfall priority order until one returns results.
     */
    suspend fun getStreamsWaterfall(episodeUrl: String): List<StreamData> {
        val scrapers = listOf(kurama, samehadaku, otakudesu, donghua, drakor)

        for (scraper in scrapers) {
            try {
                val streams = scraper.getStreams(episodeUrl)
                if (streams.isNotEmpty()) return streams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }
}
