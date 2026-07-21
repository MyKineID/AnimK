package com.animk.app.data.cache

import android.content.Context
import com.animk.app.data.model.Episode
import com.animk.app.data.model.MediaItem
import com.animk.app.data.model.StreamData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Small on-device cache for parsed provider responses. */
object ScraperCache {
    private const val PREFS = "animk_scraper_cache"
    private const val EPISODE_TTL_MS = 6 * 60 * 60 * 1000L
    private const val CATALOG_TTL_MS = 30 * 60 * 1000L
    // Stream tokens expire, therefore they are deliberately cached only briefly.
    private const val STREAM_TTL_MS = 2 * 60 * 1000L
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getEpisodes(title: String): List<Episode>? = get("episodes_v3_${key(title)}", EPISODE_TTL_MS) {
        json.decodeFromString<List<Episode>>(it)
    }

    fun getCatalog(cacheKey: String): List<MediaItem>? = get("catalog_${key(cacheKey)}", CATALOG_TTL_MS) {
        json.decodeFromString<List<MediaItem>>(it)
    }

    fun putCatalog(cacheKey: String, items: List<MediaItem>) {
        put("catalog_${key(cacheKey)}", json.encodeToString(items))
    }

    fun putEpisodes(title: String, episodes: List<Episode>) {
        put("episodes_v3_${key(title)}", json.encodeToString(episodes))
    }

    fun getStreams(episodeUrl: String): List<StreamData>? = get("streams_v5_${key(episodeUrl)}", STREAM_TTL_MS) {
        json.decodeFromString<List<StreamData>>(it)
    }

    fun putStreams(episodeUrl: String, streams: List<StreamData>) {
        put("streams_v5_${key(episodeUrl)}", json.encodeToString(streams))
    }

    private fun <T> get(key: String, ttlMs: Long, decode: (String) -> T): T? {
        return try {
            val store = prefs ?: return null
            val savedAt = store.getLong("${key}_at", 0L)
            if (savedAt == 0L || System.currentTimeMillis() - savedAt > ttlMs) return null
            store.getString(key, null)?.let(decode)
        } catch (_: Exception) {
            null
        }
    }

    private fun put(key: String, value: String) {
        prefs?.edit()
            ?.putString(key, value)
            ?.putLong("${key}_at", System.currentTimeMillis())
            ?.apply()
    }

    private fun key(value: String): String = value.lowercase().hashCode().toUInt().toString(16)
}
