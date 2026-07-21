package com.animk.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    ANIME, DONGHUA, DRAKOR, DRACHIN, JDRAMA
}

@Serializable
enum class StreamResolution {
    HD_1080p, HD_720p, SD_480p, SD_360p, UNKNOWN
}

@Serializable
enum class ServerPriority {
    HIGH, MEDIUM, LOW
}

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String,
    val backdropUrl: String? = null,
    val description: String = "",
    val averageScore: Int = 0,
    val releaseYear: Int = 0,
    val genres: List<String> = emptyList(),
    val isTrending: Boolean = false,
    val episodes: List<Episode> = emptyList(),
    val matchPercentage: Int = 98,
    val ageRating: String = "16+",
    val quality: String = "HD"
)

@Serializable
data class EpisodeSource(
    val providerKey: String,
    val providerName: String,
    val sourceUrl: String
)

@Serializable
data class Episode(
    val id: String,
    val sourceUrl: String,
    val episodeNumber: Float,
    val title: String,
    val thumbnailUrl: String? = null,
    val description: String = "",
    val duration: String = "24m",
    /** Same episode from other active providers, merged by ScraperRepository. */
    val sources: List<EpisodeSource> = emptyList()
)

@Serializable
data class StreamData(
    val serverName: String,
    val streamUrl: String,
    val isIframe: Boolean,
    val resolution: StreamResolution,
    val priority: ServerPriority,
    val additionalHeaders: Map<String, String> = emptyMap(),
    /** Display-only provider name, so identical titles can expose every source. */
    val providerName: String = ""
)
