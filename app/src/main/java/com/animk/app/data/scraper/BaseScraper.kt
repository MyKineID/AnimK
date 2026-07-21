package com.animk.app.data.scraper

import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.StreamData

interface BaseScraper {
    val sourceName: String
    val baseUrl: String

    suspend fun search(query: String): List<MediaItem>
    suspend fun getEpisodes(mediaUrl: String): List<Episode>
    suspend fun getStreams(episodeUrl: String): List<StreamData>
}
