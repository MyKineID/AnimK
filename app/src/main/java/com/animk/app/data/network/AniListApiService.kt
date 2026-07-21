package com.animk.app.data.network

import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AniListApiService {
    private val client = OkHttpClientBuilder.buildUnsafeClient()
    private val graphqlUrl = "https://graphql.anilist.co"

    suspend fun getTrendingAnime(page: Int = 1, perPage: Int = 10): List<MediaItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(sort: TRENDING_DESC, type: ANIME) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  bannerImage
                  description
                  episodes
                  genres
                  averageScore
                  seasonYear
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("page", page)
                put("perPage", perPage)
            })
        }

        executeGraphQL(jsonBody.toString(), isTrending = true)
    }

    suspend fun searchAnime(searchQuery: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 20) {
                media(search: ${'$'}search, sort: POPULARITY_DESC, type: ANIME) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  bannerImage
                  description
                  episodes
                  genres
                  averageScore
                  seasonYear
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("search", searchQuery)
            })
        }

        executeGraphQL(jsonBody.toString(), isTrending = false)
    }

    private fun executeGraphQL(jsonString: String, isTrending: Boolean): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()
        try {
            val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(graphqlUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return emptyList()
            val rootObj = JSONObject(responseBody)
            val dataObj = rootObj.optJSONObject("data") ?: return emptyList()
            val pageObj = dataObj.optJSONObject("Page") ?: return emptyList()
            val mediaArray = pageObj.optJSONArray("media") ?: return emptyList()

            for (i in 0 until mediaArray.length()) {
                val item = mediaArray.getJSONObject(i)
                val id = item.optInt("id").toString()
                val titleObj = item.optJSONObject("title")
                val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() }
                    ?: titleObj?.optString("romaji")
                    ?: titleObj?.optString("native")
                    ?: "Anime $id"

                val coverObj = item.optJSONObject("coverImage")
                val posterUrl = coverObj?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                    ?: coverObj?.optString("large")
                    ?: ""
                val bannerUrl = item.optString("bannerImage").takeIf { it.isNotBlank() }

                val descRaw = item.optString("description", "")
                val description = descRaw.replace(Regex("<[^>]*>"), "").trim()

                val score = item.optInt("averageScore", 0)
                val year = item.optInt("seasonYear", 2024)

                val genresArray = item.optJSONArray("genres")
                val genres = mutableListOf<String>()
                if (genresArray != null) {
                    for (j in 0 until genresArray.length()) {
                        genres.add(genresArray.getString(j))
                    }
                }

                mediaList.add(
                    MediaItem(
                        id = id,
                        title = title,
                        type = MediaType.ANIME,
                        posterUrl = posterUrl,
                        backdropUrl = bannerUrl ?: posterUrl,
                        description = description.ifEmpty { "High rated anime from AniList." },
                        averageScore = score,
                        releaseYear = if (year > 0) year else 2024,
                        genres = if (genres.isNotEmpty()) genres else listOf("Action", "Fantasy"),
                        isTrending = isTrending,
                        matchPercentage = if (score > 0) score else 95
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaList
    }
}
