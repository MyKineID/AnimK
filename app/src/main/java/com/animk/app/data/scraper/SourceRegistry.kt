package com.animk.app.data.scraper

import com.animk.app.data.model.MediaType
import com.animk.app.data.remoteconfig.ProviderConfig

/**
 * Registry that maps remote config provider keys to their scraper implementations.
 */
object SourceRegistry {
    private val sources: Map<String, BaseScraper> = listOf(
        KuramanimeScraper(),
        SamehadakuScraper(),
        OtakudesuScraper(),
        NimegamiScraper(),
        AnoboyLegacyScraper(),
        DonghuaScraper(),
        DrakorScraper()
    ).associateBy { it.sourceKey }

    /**
     * Uses a dedicated scraper when one exists. Verified selector-driven
     * fallback providers can otherwise be activated from Director remotely.
     */
    fun getSource(key: String, config: ProviderConfig? = null): BaseScraper? =
        sources[key] ?: config?.let {
            val type = it.mediaTypes.firstNotNullOfOrNull { name ->
                MediaType.entries.firstOrNull { mediaType -> mediaType.name.equals(name, ignoreCase = true) }
            } ?: MediaType.ANIME
            GenericProviderScraper(key, it.displayName, type)
        }

    /** All registered scraper implementations. */
    fun getAllSources(): Collection<BaseScraper> = sources.values

    /** Keys of all registered scrapers. */
    fun getAllKeys(): Set<String> = sources.keys
}
