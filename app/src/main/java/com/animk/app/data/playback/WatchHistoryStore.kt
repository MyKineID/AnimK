package com.animk.app.data.playback

import android.content.Context
import com.animk.app.data.model.MediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WatchHistoryEntry(
    val media: MediaItem,
    val episodeSourceUrl: String,
    val episodeTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long
)

/** Persists resume points and the last 30 watched episodes on-device. */
object WatchHistoryStore {
    private const val PREFS = "animk_watch_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 30
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun history(): List<WatchHistoryEntry> = read().sortedByDescending { it.updatedAtMs }

    fun positionFor(episodeSourceUrl: String): Long = read()
        .firstOrNull { it.episodeSourceUrl == episodeSourceUrl }
        ?.positionMs ?: 0L

    fun saveProgress(
        media: MediaItem,
        episodeSourceUrl: String,
        episodeTitle: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (episodeSourceUrl.isBlank() || positionMs < 1_000L) return
        val resumePosition = if (durationMs > 0 && positionMs >= durationMs - 10_000L) 0L else positionMs
        val entry = WatchHistoryEntry(
            media = media,
            episodeSourceUrl = episodeSourceUrl,
            episodeTitle = episodeTitle,
            positionMs = resumePosition,
            durationMs = durationMs,
            updatedAtMs = System.currentTimeMillis()
        )
        val updated = (read().filterNot { it.episodeSourceUrl == episodeSourceUrl } + entry)
            .sortedByDescending { it.updatedAtMs }
            .take(MAX_ENTRIES)
        prefs?.edit()?.putString(KEY_ENTRIES, json.encodeToString(updated))?.apply()
    }

    private fun read(): List<WatchHistoryEntry> {
        return try {
            val raw = prefs?.getString(KEY_ENTRIES, null) ?: return emptyList()
            json.decodeFromString<List<WatchHistoryEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
