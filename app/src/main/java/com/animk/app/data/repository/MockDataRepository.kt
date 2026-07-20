package com.animk.app.data.repository

import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType

object MockDataRepository {

    private val sampleEpisodes = listOf(
        Episode(
            id = "ep1",
            episodeNumber = 1,
            title = "Episode 1: The Beginning",
            duration = "24m",
            thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&q=80",
            description = "The journey begins as our protagonist discovers an ancient power."
        ),
        Episode(
            id = "ep2",
            episodeNumber = 2,
            title = "Episode 2: Awakening Power",
            duration = "23m",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80",
            description = "Faced with unexpected enemies, secret abilities are unleashed."
        ),
        Episode(
            id = "ep3",
            episodeNumber = 3,
            title = "Episode 3: The Dark Alliance",
            duration = "25m",
            thumbnailUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&q=80",
            description = "A surprising pact is forged in the shadows of the academy."
        ),
        Episode(
            id = "ep4",
            episodeNumber = 4,
            title = "Episode 4: Clash of Titans",
            duration = "24m",
            thumbnailUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&q=80",
            description = "An epic battle determines the fate of the outer realm."
        )
    )

    val heroFeatured = MediaItem(
        id = "hero_1",
        title = "Solo Leveling: Arise",
        type = MediaType.ANIME,
        posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&q=80",
        backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
        description = "In a world where hunters must battle deadly monsters, weak Sung Jinwoo gains the secret to leveling up infinitely.",
        matchPercentage = 99,
        releaseYear = 2024,
        ageRating = "16+",
        quality = "4K HDR",
        genres = listOf("Action", "Fantasy", "Supernatural"),
        episodes = sampleEpisodes,
        isTop10 = true,
        topRank = 1,
        isTrending = true
    )

    val animeList = listOf(
        heroFeatured,
        MediaItem(
            id = "anime_2",
            title = "Demon Slayer: Hashira Training",
            type = MediaType.ANIME,
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1000&q=80",
            description = "Tanjiro visits the Hashira Stone Hashira Himejima to prepare for upcoming battles.",
            matchPercentage = 97,
            releaseYear = 2024,
            ageRating = "16+",
            quality = "1080p",
            genres = listOf("Action", "Demon", "Shounen"),
            episodes = sampleEpisodes,
            isTop10 = true,
            topRank = 2,
            isTrending = true
        ),
        MediaItem(
            id = "anime_3",
            title = "Jujutsu Kaisen Season 2",
            type = MediaType.ANIME,
            posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1000&q=80",
            description = "GoJo's past comes to light alongside the Shibuya Incident arc.",
            matchPercentage = 98,
            releaseYear = 2023,
            ageRating = "18+",
            quality = "1080p",
            genres = listOf("Dark Fantasy", "Supernatural"),
            episodes = sampleEpisodes,
            isTop10 = true,
            topRank = 3,
            isTrending = true
        ),
        MediaItem(
            id = "anime_4",
            title = "Attack on Titan: The Final Chapters",
            type = MediaType.ANIME,
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            description = "The fate of humanity hangs in the balance as Eren proceeds with the Rumbling.",
            matchPercentage = 96,
            releaseYear = 2023,
            ageRating = "18+",
            quality = "1080p",
            genres = listOf("Action", "Drama", "Mystery"),
            episodes = sampleEpisodes,
            isNewRelease = true
        )
    )

    val donghuaList = listOf(
        MediaItem(
            id = "dh_1",
            title = "Soul Land II: Peerless Tang Sect",
            type = MediaType.DONGHUA,
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1000&q=80",
            description = "Ten thousand years after the founding of Tang Sect, a new genius emerges in Shrek Academy.",
            matchPercentage = 95,
            releaseYear = 2024,
            ageRating = "13+",
            quality = "1080p",
            genres = listOf("Xianxia", "Action", "Romance"),
            episodes = sampleEpisodes,
            isTop10 = true,
            topRank = 4,
            isTrending = true
        ),
        MediaItem(
            id = "dh_2",
            title = "Perfect World (Wanmei Shijie)",
            type = MediaType.DONGHUA,
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1000&q=80",
            description = "Born into a unique world where villages fight for power, Shi Hao strives to master cultivation.",
            matchPercentage = 94,
            releaseYear = 2024,
            ageRating = "16+",
            quality = "1080p",
            genres = listOf("Cultivation", "Adventure"),
            episodes = sampleEpisodes,
            isTrending = true
        ),
        MediaItem(
            id = "dh_3",
            title = "Battle Through The Heavens",
            type = MediaType.DONGHUA,
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            description = "Xiao Yan regains his martial talents and journeys across the Dou Qi continent.",
            matchPercentage = 98,
            releaseYear = 2024,
            ageRating = "16+",
            quality = "4K",
            genres = listOf("Action", "Martial Arts"),
            episodes = sampleEpisodes,
            isTop10 = true,
            topRank = 5
        )
    )

    val drakorList = listOf(
        MediaItem(
            id = "dk_1",
            title = "Queen of Tears",
            type = MediaType.DRAKOR,
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1000&q=80",
            description = "The queen of department stores and her small-town husband weather a marital crisis until love unexpectedly blooms again.",
            matchPercentage = 99,
            releaseYear = 2024,
            ageRating = "15+",
            quality = "1080p",
            genres = listOf("Romance", "Drama", "Comedy"),
            episodes = sampleEpisodes,
            isTop10 = true,
            topRank = 6,
            isTrending = true
        ),
        MediaItem(
            id = "dk_2",
            title = "Lovely Runner",
            type = MediaType.DRAKOR,
            posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1000&q=80",
            description = "A devoted fan travels back in time to save her favorite idol from a tragic fate.",
            matchPercentage = 97,
            releaseYear = 2024,
            ageRating = "15+",
            quality = "1080p",
            genres = listOf("Time Travel", "Romance", "Fantasy"),
            episodes = sampleEpisodes,
            isTrending = true
        ),
        MediaItem(
            id = "dk_3",
            title = "All of Us Are Dead",
            type = MediaType.DRAKOR,
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            description = "A high school becomes ground zero for a zombie virus outbreak. Trapped students must fight their way out.",
            matchPercentage = 95,
            releaseYear = 2022,
            ageRating = "18+",
            quality = "1080p",
            genres = listOf("Thriller", "Horror", "Action"),
            episodes = sampleEpisodes,
            isNewRelease = true
        )
    )

    fun getAllMedia(): List<MediaItem> = animeList + donghuaList + drakorList
}
