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
