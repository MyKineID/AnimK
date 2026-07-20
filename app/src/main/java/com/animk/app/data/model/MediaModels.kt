package com.animk.app.data.model

enum class MediaType(val displayName: String) {
    ANIME("Anime"),
    DONGHUA("Donghua"),
    DRAKOR("Drakor")
}

data class Episode(
    val id: String,
    val episodeNumber: Int,
    val title: String,
    val duration: String,
    val thumbnailUrl: String,
    val description: String = ""
)

data class MediaItem(
    val id: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String,
    val backdropUrl: String,
    val description: String,
    val matchPercentage: Int = 98,
    val releaseYear: Int = 2024,
    val ageRating: String = "16+",
    val quality: String = "1080p",
    val genres: List<String>,
    val episodes: List<Episode> = emptyList(),
    val isTop10: Boolean = false,
    val topRank: Int? = null,
    val isTrending: Boolean = false,
    val isNewRelease: Boolean = false,
    val isMyList: Boolean = false
)
