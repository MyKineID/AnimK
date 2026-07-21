package com.animk.app.data.scraper

import com.animk.app.data.model.StreamData

class AnimeOrchestrator(
    private val kurama: BaseScraper = KuramanimeScraper(),
    private val samehadaku: BaseScraper = SamehadakuScraper(),
    private val otakudesu: BaseScraper = OtakudesuScraper()
) {
    suspend fun getStreamsWaterfall(episodeTitle: String): List<StreamData> {
        try {
            val streams = kurama.getStreams(episodeTitle)
            if (streams.isNotEmpty()) return streams
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val streams = samehadaku.getStreams(episodeTitle)
            if (streams.isNotEmpty()) return streams
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return try {
            otakudesu.getStreams(episodeTitle)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
