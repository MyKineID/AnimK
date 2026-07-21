package com.animk.app.data.scraper

/**
 * Registry that maps remote config provider keys to their scraper implementations.
 */
object SourceRegistry {
    private val sources: Map<String, BaseScraper> = listOf(
        KuramanimeScraper(),
        SamehadakuScraper(),
        OtakudesuScraper(),
        DonghuaScraper(),
        DrakorScraper()
    ).associateBy { it.sourceKey }

    /** Look up a scraper by its provider key (e.g. "otakudesu", "kuramanime"). */
    fun getSource(key: String): BaseScraper? = sources[key]

    /** All registered scraper implementations. */
    fun getAllSources(): Collection<BaseScraper> = sources.values

    /** Keys of all registered scrapers. */
    fun getAllKeys(): Set<String> = sources.keys
}
